/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageChannel;

/**
 * Represents a message channel.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageChannelNode extends IntegrationNode implements SendTimersAware {

	private @Nullable Supplier<SendTimers> sendTimers;

	public MessageChannelNode(int nodeId, String name, MessageChannel channel) {
		super(nodeId, name, channel);
	}

	@Nullable
	public SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

}
