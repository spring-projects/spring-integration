/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

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

	private final String errors;

	public ErrorCapableDiscardingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			String output, String discards, String errors) {

		super(nodeId, name, handler, input, output, discards);
		this.errors = errors;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
