/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.integration.util.CallerBlocksPolicy;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * An {@link AbstractDispatcher} implementation for distributing messages to
 * dedicated threads according to the key determined by the provided function against
 * the message to dispatch.
 * <p>
 * Every partition, created by this class, is a {@link UnicastingDispatcher}
 * delegate based on a single thread {@link Executor}.
 * <p>
 * The number of partitions should be a reasonable value for the application environment
 * since every partition is based on a dedicated thread for message processing.
 * <p>
 * The rest of the logic is similar to {@link UnicastingDispatcher} behavior.
 *
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 6.1
 */
public class PartitionedDispatcher extends AbstractDispatcher {

	private final Map<Integer, UnicastingDispatcher> partitions = new HashMap<>();

	private final List<ExecutorService> executors = new ArrayList<>();

	private final int partitionCount;

	private final Function<Message<?>, Object> partitionKeyFunction;

	private ThreadFactory threadFactory = new CustomizableThreadFactory("partition-thread-");

	private Predicate<Exception> failoverStrategy = (exception) -> true;

	@Nullable
	private LoadBalancingStrategy loadBalancingStrategy;

	private ErrorHandler errorHandler;

	private MessageHandlingTaskDecorator messageHandlingTaskDecorator = task -> task;

	private final Lock lock = new ReentrantLock();

	private int workerQueueSize;

	/**
	 * Instantiate based on a provided number of partitions and function for partition key against
	 * the message to dispatch.
	 * @param partitionCount the number of partitions in this channel.
	 * @param partitionKeyFunction the function to resolve a partition key against the message
	 * to dispatch.
	 */
	public PartitionedDispatcher(int partitionCount, Function<Message<?>, Object> partitionKeyFunction) {
		Assert.isTrue(partitionCount > 0, "'partitionCount' must be greater than 0");
		Assert.notNull(partitionKeyFunction, "'partitionKeyFunction' must not be null");
		this.partitionKeyFunction = partitionKeyFunction;
		this.partitionCount = partitionCount;
	}

	/**
	 * Set a {@link ThreadFactory} for executors per partitions.
	 * Defaults to the {@link CustomizableThreadFactory} based on a {@code partition-thread-} prefix.
	 * @param threadFactory the {@link ThreadFactory} to use.
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		Assert.notNull(threadFactory, "'threadFactory' must not be null");
		this.threadFactory = threadFactory;
	}

	/**
	 * Specify whether partition dispatchers should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		setFailoverStrategy((exception) -> failover);
	}

	/**
	 * Configure a strategy whether the channel's dispatcher should have failover enabled
	 * for the exception thrown.
	 * Overrides {@link #setFailover(boolean)} option.
	 * In other words: or this, or that option has to be set.
	 * @param failoverStrategy The failover boolean.
	 * @since 6.3
	 */
	public void setFailoverStrategy(Predicate<Exception> failoverStrategy) {
		Assert.notNull(failoverStrategy, "'failoverStrategy' must not be null");
		this.failoverStrategy = failoverStrategy;
	}

	/**
	 * Provide a {@link LoadBalancingStrategy} for partition dispatchers.
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 */
	public void setLoadBalancingStrategy(@Nullable LoadBalancingStrategy loadBalancingStrategy) {
		this.loadBalancingStrategy = loadBalancingStrategy;
	}

	/**
	 * Provide a {@link ErrorHandler} for wrapping partition {@link Executor}
	 * to the {@link ErrorHandlingTaskExecutor}.
	 * @param errorHandler the {@link ErrorHandler} to use.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set a {@link MessageHandlingTaskDecorator} to wrap a message handling task into some
	 * addition logic, e.g. message channel may provide an interception for its operations.
	 * @param messageHandlingTaskDecorator the {@link MessageHandlingTaskDecorator} to use.
	 */
	public void setMessageHandlingTaskDecorator(MessageHandlingTaskDecorator messageHandlingTaskDecorator) {
		Assert.notNull(messageHandlingTaskDecorator, "'messageHandlingTaskDecorator' must not be null.");
		this.messageHandlingTaskDecorator = messageHandlingTaskDecorator;
	}

	/**
	 * Provide a size of the queue in the partition executor's worker.
	 * Default to zero.
	 * @param workerQueueSize the size of the partition executor's worker queue.
	 * @since 6.4.10
	 */
	public void setWorkerQueueSize(int workerQueueSize) {
		Assert.isTrue(workerQueueSize >= 0, "'workerQueueSize' must be greater than or equal to 0.");
		this.workerQueueSize = workerQueueSize;
	}

	/**
	 * Shutdown this dispatcher on application close.
	 * The partition executors are shutdown and internal state of this instance is cleared.
	 */
	public void shutdown() {
		this.executors.forEach(ExecutorService::shutdown);
		this.executors.clear();
		this.partitions.clear();
	}

	@Override
	public boolean dispatch(Message<?> message) {
		populatedPartitions();
		int partition = Math.abs(this.partitionKeyFunction.apply(message).hashCode()) % this.partitionCount;
		UnicastingDispatcher partitionDispatcher = this.partitions.get(partition);
		return partitionDispatcher.dispatch(message);
	}

	private void populatedPartitions() {
		if (this.partitions.isEmpty()) {
			this.lock.lock();
			try {
				if (this.partitions.isEmpty()) {
					Map<Integer, UnicastingDispatcher> partitionsToUse = new HashMap<>();
					for (int i = 0; i < this.partitionCount; i++) {
						partitionsToUse.put(i, newPartition());
					}
					this.partitions.putAll(partitionsToUse);
				}
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	private UnicastingDispatcher newPartition() {
		BlockingQueue<Runnable> workQueue =
				this.workerQueueSize == 0
						? new SynchronousQueue<>()
						: new LinkedBlockingQueue<>(this.workerQueueSize);
		ExecutorService executor =
				new ThreadPoolExecutor(1, 1,
						0L, TimeUnit.MILLISECONDS,
						workQueue,
						this.threadFactory,
						new CallerBlocksPolicy(Long.MAX_VALUE));
		this.executors.add(executor);
		DelegateDispatcher delegateDispatcher =
				new DelegateDispatcher(new ErrorHandlingTaskExecutor(executor, this.errorHandler));
		delegateDispatcher.setFailoverStrategy(this.failoverStrategy);
		delegateDispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		delegateDispatcher.setMessageHandlingTaskDecorator(this.messageHandlingTaskDecorator);
		return delegateDispatcher;
	}

	private final class DelegateDispatcher extends UnicastingDispatcher {

		DelegateDispatcher(Executor executor) {
			super(executor);
		}

		@Override
		protected Set<MessageHandler> getHandlers() {
			return PartitionedDispatcher.this.getHandlers();
		}

		@Override
		protected boolean tryOptimizedDispatch(Message<?> message) {
			return PartitionedDispatcher.this.tryOptimizedDispatch(message);
		}

	}

}
