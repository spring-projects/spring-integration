/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler that can produce errors (pollable).
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class ErrorCapableMessageHandlerNode extends MessageHandlerNode implements ErrorCapableNode {

	private final String errors;

	public ErrorCapableMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			String output, String errors) {

		super(nodeId, name, handler, input, output);
		this.errors = errors;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
