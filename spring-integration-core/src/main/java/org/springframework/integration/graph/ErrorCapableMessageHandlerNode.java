/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler that can produce errors (pollable).
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableMessageHandlerNode extends MessageHandlerNode implements ErrorCapableNode {

	private final @Nullable String errors;

	public ErrorCapableMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			@Nullable String output, @Nullable String errors) {

		super(nodeId, name, handler, input, output);
		this.errors = errors;
	}

	@Override
	public @Nullable String getErrors() {
		return this.errors;
	}

}
