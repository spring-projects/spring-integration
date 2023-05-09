/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.PartitionedDispatcher;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * An {@link AbstractExecutorChannel} implementation for partitioned messages dispatching.
 * Requires a number of partitions where each of them is backed by a dedicated thread.
 * The {@code partitionKeyFunction} is used to determine to which partition the message
 * has to be dispatched.
 * <p>
 * The actual dispatching and threading logic in implemented in the {@link PartitionedDispatcher}.
 * <p>
 * The default {@link ThreadFactory} is based on a bean name of this channel plus {@code -partition-thread-}.
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

	public PartitionedChannel(int partitionCount, Function<Message<?>, Object> partitionKeyFunction) {
		super(null);
		this.dispatcher = new PartitionedDispatcher(partitionCount, partitionKeyFunction);
	}

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

	public void setLoadBalancingStrategy(@Nullable LoadBalancingStrategy loadBalancingStrategy) {
		getDispatcher().setLoadBalancingStrategy(loadBalancingStrategy);
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
