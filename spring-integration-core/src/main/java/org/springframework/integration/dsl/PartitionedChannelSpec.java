/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
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

	@Override
	protected PartitionedChannel doGet() {
		if (this.partitionKeyFunction != null) {
			this.channel = new PartitionedChannel(this.partitionCount, this.partitionKeyFunction);
		}
		else {
			this.channel = new PartitionedChannel(this.partitionCount);
		}
		this.channel.setLoadBalancingStrategy(this.loadBalancingStrategy);
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
