/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageHandlerNode extends EndpointNode implements SendTimersAware {

	private final String input;

	private @Nullable Supplier<SendTimers> sendTimers;

	public MessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, @Nullable String output) {
		super(nodeId, name, handler, output);
		this.input = input;
	}

	public String getInput() {
		return this.input;
	}

	public @Nullable SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

}
