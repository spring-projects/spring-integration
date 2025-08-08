/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.Collection;

import org.springframework.messaging.MessageHandler;

/**
 * Represents an endpoint that can route to multiple channels.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class RoutingMessageHandlerNode extends MessageHandlerNode {

	private final Collection<String> routes;

	public RoutingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output,
			Collection<String> routes) {

		super(nodeId, name, handler, input, output);
		this.routes = routes;
	}

	public Collection<String> getRoutes() {
		return this.routes;
	}

}
