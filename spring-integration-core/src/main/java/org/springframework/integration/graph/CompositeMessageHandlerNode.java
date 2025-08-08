/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.MessageHandler;

/**
 * Represents a composite message handler.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class CompositeMessageHandlerNode extends MessageHandlerNode {

	private final List<InnerHandler> handlers = new ArrayList<>();

	public CompositeMessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output,
			List<InnerHandler> handlers) {

		super(nodeId, name, handler, input, output);
		this.handlers.addAll(handlers);
	}

	public List<InnerHandler> getHandlers() {
		return this.handlers;
	}

	public static class InnerHandler {

		private final String name;

		private final String type;

		public InnerHandler(String name, String type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
		}

	}

}
