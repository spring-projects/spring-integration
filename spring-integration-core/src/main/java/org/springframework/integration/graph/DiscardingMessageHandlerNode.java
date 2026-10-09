/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHandler;

/**
 * Represents an endpoint that has a discard channel.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class DiscardingMessageHandlerNode extends MessageHandlerNode {

	private final @Nullable String discards;

	public DiscardingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			@Nullable String output, @Nullable String discards) {

		super(nodeId, name, handler, input, output);
		this.discards = discards;
	}

	public @Nullable String getDiscards() {
		return this.discards;
	}

}
