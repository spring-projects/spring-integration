/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHandler;

/**
 * Represents a composite message handler.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class CompositeMessageHandlerNode extends MessageHandlerNode {

	private final List<InnerHandler> handlers;

	public CompositeMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			@Nullable String output, List<InnerHandler> handlers) {

		super(nodeId, name, handler, input, output);
		this.handlers = new ArrayList<>(handlers);
	}

	public List<InnerHandler> getHandlers() {
		return this.handlers;
	}

	public record InnerHandler(String name, String type) {

	}

}
