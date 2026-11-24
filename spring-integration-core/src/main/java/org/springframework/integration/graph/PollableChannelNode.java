/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

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

	private @Nullable Supplier<ReceiveCounters> receiveCounters;

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
