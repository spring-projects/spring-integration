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

package org.springframework.integration.dsl;

import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import org.springframework.integration.channel.PartitionedChannel;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link LoadBalancingChannelSpec} implementation for the {@link PartitionedChannel}.
 *
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class PartitionedChannelSpec extends LoadBalancingChannelSpec<PartitionedChannelSpec, PartitionedChannel> {

	private final int partitionCount;

	@Nullable
	private Function<Message<?>, Object> partitionKeyFunction;

	@Nullable
	private ThreadFactory threadFactory;

	private int workerQueueSize;

	protected PartitionedChannelSpec(int partitionCount) {
		this.partitionCount = partitionCount;
	}

	public PartitionedChannelSpec partitionKey(Function<Message<?>, Object> partitionKeyFunction) {
		this.partitionKeyFunction = partitionKeyFunction;
		return this;
	}

	public PartitionedChannelSpec threadFactory(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
		return this;
	}

	/**
	 * Provide a size of the queue in the partition executor's worker.
	 * Default to zero.
	 * @param workerQueueSize the size of the partition executor's worker queue.
	 * @return the spec.
	 * @since 6.4.10
	 */
	public PartitionedChannelSpec workerQueueSize(int workerQueueSize) {
		this.workerQueueSize = workerQueueSize;
		return this;
	}

	@Override
	protected PartitionedChannel doGet() {
		if (this.partitionKeyFunction != null) {
			this.channel = new PartitionedChannel(this.partitionCount, this.partitionKeyFunction);
		}
		else {
			this.channel = new PartitionedChannel(this.partitionCount);
		}
		this.channel.setLoadBalancingStrategy(this.loadBalancingStrategy);
		this.channel.setWorkerQueueSize(this.workerQueueSize);
		if (this.failoverStrategy != null) {
			this.channel.setFailoverStrategy(this.failoverStrategy);
		}
		if (this.maxSubscribers != null) {
			this.channel.setMaxSubscribers(this.maxSubscribers);
		}
		if (this.threadFactory != null) {
			this.channel.setThreadFactory(this.threadFactory);
		}
		return super.doGet();
	}

}
