/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.Collection;

import org.springframework.messaging.MessageHandler;

/**
 * Represents an endpoint that can route to multiple channels and can emit errors
 * (pollable endpoint).
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableRoutingNode extends RoutingMessageHandlerNode implements ErrorCapableNode {

	private final String errors;

	public ErrorCapableRoutingNode(int nodeId, String name, MessageHandler handler, String input, String output,
			String errors, Collection<String> routes) {

		super(nodeId, name, handler, input, output, routes);
		this.errors = errors;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
