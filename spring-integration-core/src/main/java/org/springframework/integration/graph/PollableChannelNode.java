/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * Represents a pollable channel.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public class PollableChannelNode extends MessageChannelNode implements ReceiveCountersAware {

	private Supplier<ReceiveCounters> receiveCounters;

	public PollableChannelNode(int nodeId, String name, MessageChannel channel) {
		super(nodeId, name, channel);
	}

	@Nullable
	public ReceiveCounters getReceiveCounters() {
		return this.receiveCounters != null ? this.receiveCounters.get() : null;
	}

	@Override
	public void receiveCounters(Supplier<ReceiveCounters> counters) {
		this.receiveCounters = counters;
	}

}
