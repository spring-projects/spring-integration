/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
