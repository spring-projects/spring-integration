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

package org.springframework.integration.channel;

import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.PartitionedDispatcher;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * An {@link AbstractExecutorChannel} implementation for partitioned message dispatching.
 * Requires a number of partitions where each of them is backed by a dedicated thread.
 * The {@code partitionKeyFunction} is used to determine to which partition the message
 * has to be dispatched.
 * By default, the {@link IntegrationMessageHeaderAccessor#CORRELATION_ID} message header is used
 * for a partition key.
 * <p>
 * The actual dispatching and threading logic is implemented in the {@link PartitionedDispatcher}.
 * <p>
 * The default {@link ThreadFactory} is based on the bean name of this channel plus {@code -partition-thread-}.
 * Thus, every thread name will reflect a partition it belongs to.
 * <p>
 * The rest of the logic is similar to the {@link ExecutorChannel}, which includes:
 * - load balancing for subscribers;
 * - fail-over and error handling;
 * - channel operations intercepting.
 *
 * @author Artem Bilan
 *
 * @since 6.1
 *
 * @see PartitionedDispatcher
 */
public class PartitionedChannel extends AbstractExecutorChannel {

	@Nullable
	private ThreadFactory threadFactory;

	/**
	 * Instantiate based on a provided number of partitions and function resolving a partition key from
	 * the {@link IntegrationMessageHeaderAccessor#CORRELATION_ID} message header.
	 * @param partitionCount the number of partitions in this channel.
	 * sent to this channel.
	 */
	public PartitionedChannel(int partitionCount) {
		this(partitionCount, (message) -> message.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID));
	}

	/**
	 * Instantiate based on a provided number of partitions and function for a partition key against
	 * the message.
	 * @param partitionCount the number of partitions in this channel.
	 * @param partitionKeyFunction the function to resolve a partition key against the message
	 * sent to this channel.
	 */
	public PartitionedChannel(int partitionCount, Function<Message<?>, Object> partitionKeyFunction) {
		super(null);
		this.dispatcher = new PartitionedDispatcher(partitionCount, partitionKeyFunction);
	}

	/**
	 * Set a {@link ThreadFactory} for executors per partitions.
	 * Propagated down to the {@link PartitionedDispatcher}.
	 * Defaults to the {@link CustomizableThreadFactory} based on the bean name
	 * of this channel plus {@code -partition-thread-}.
	 * @param threadFactory the {@link ThreadFactory} to use.
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		Assert.notNull(threadFactory, "'threadFactory' must not be null");
		this.threadFactory = threadFactory;
	}

	/**
	 * Specify whether the channel's dispatcher should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		getDispatcher().setFailover(failover);
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
		getDispatcher().setFailoverStrategy(failoverStrategy);
	}

	/**
	 * Provide a {@link LoadBalancingStrategy} for the {@link PartitionedDispatcher}.
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 */
	public void setLoadBalancingStrategy(@Nullable LoadBalancingStrategy loadBalancingStrategy) {
		getDispatcher().setLoadBalancingStrategy(loadBalancingStrategy);
	}

	/**
	 * Provide a size of the queue in the partition executor's worker.
	 * Default to zero.
	 * @param workerQueueSize the size of the partition executor's worker queue.
	 * @since 6.4.10
	 */
	public void setWorkerQueueSize(int workerQueueSize) {
		getDispatcher().setWorkerQueueSize(workerQueueSize);
	}

	@Override
	protected PartitionedDispatcher getDispatcher() {
		return (PartitionedDispatcher) this.dispatcher;
	}

	@Override
	protected void onInit() {
		super.onInit();

		if (this.threadFactory == null) {
			this.threadFactory = new CustomizableThreadFactory(getComponentName() + "-partition-thread-");
		}
		PartitionedDispatcher partitionedDispatcher = getDispatcher();
		partitionedDispatcher.setThreadFactory(this.threadFactory);

		if (this.maxSubscribers == null) {
			partitionedDispatcher.setMaxSubscribers(getIntegrationProperties().getChannelsMaxUnicastSubscribers());
		}

		partitionedDispatcher.setErrorHandler(ChannelUtils.getErrorHandler(getBeanFactory()));

		partitionedDispatcher.setMessageHandlingTaskDecorator(task -> {
			if (this.executorInterceptorsSize > 0) {
				return new MessageHandlingTask(task);
			}
			else {
				return task;
			}
		});

	}

	@Override
	public void destroy() {
		super.destroy();
		getDispatcher().shutdown();
	}

}
