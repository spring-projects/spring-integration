/*
 * Copyright 2016-present the original author or authors.
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
