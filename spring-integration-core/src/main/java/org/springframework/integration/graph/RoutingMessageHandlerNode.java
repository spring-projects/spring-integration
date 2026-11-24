/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

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

	public RoutingMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input,
			@Nullable String output, Collection<String> routes) {

		super(nodeId, name, handler, input, output);
		this.routes = routes;
	}

	public Collection<String> getRoutes() {
		return this.routes;
	}

}
