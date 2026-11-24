/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
