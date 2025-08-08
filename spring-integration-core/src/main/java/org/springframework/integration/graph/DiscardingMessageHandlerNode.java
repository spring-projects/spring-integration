/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

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

	private final String discards;

	public DiscardingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output,
			String discards) {

		super(nodeId, name, handler, input, output);
		this.discards = discards;
	}

	public String getDiscards() {
		return this.discards;
	}

}
