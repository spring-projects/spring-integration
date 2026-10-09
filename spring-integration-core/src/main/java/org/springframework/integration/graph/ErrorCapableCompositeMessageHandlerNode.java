/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.CompositeMessageHandler;

/**
 * Represents a composite message handler that can emit error messages
 * (pollable endpoint).
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableCompositeMessageHandlerNode extends CompositeMessageHandlerNode implements ErrorCapableNode {

	private final @Nullable String errors;

	public ErrorCapableCompositeMessageHandlerNode(int nodeId, String name, CompositeMessageHandler handler,
			String input, @Nullable String output, @Nullable String errors, List<InnerHandler> handlers) {

		super(nodeId, name, handler, input, output, handlers);
		this.errors = errors;
	}

	@Override
	public @Nullable String getErrors() {
		return this.errors;
	}

}
