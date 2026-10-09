/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHandler;

/**
 * Represents an endpoint that has a discard channel and can emit errors
 * (pollable endpoint).
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableDiscardingMessageHandlerNode extends DiscardingMessageHandlerNode implements ErrorCapableNode {

	private final @Nullable String errors;

	public ErrorCapableDiscardingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			@Nullable String output, @Nullable String discards, @Nullable String errors) {

		super(nodeId, name, handler, input, output, discards);
		this.errors = errors;
	}

	@Override
	public @Nullable String getErrors() {
		return this.errors;
	}

}
