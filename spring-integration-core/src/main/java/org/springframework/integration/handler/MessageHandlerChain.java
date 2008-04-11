/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.integration.message.Message;

/**
 * A message handler implementation that passes incoming messages through a
 * chain of handlers. The chain will be broken by any handler throwing an
 * exception or returning <em>null</em>.
 * 
 * @author Mark Fisher
 */
public class MessageHandlerChain implements MessageHandler {

	private final List<MessageHandler> handlers = new CopyOnWriteArrayList<MessageHandler>();


	/**
	 * Add a handler to the end of the chain.
	 */
	public void add(MessageHandler handler) {
		this.handlers.add(handler);
	}

	/**
	 * Add a handler to the chain at the specified index.
	 */
	public void add(int index, MessageHandler handler) {
		this.handlers.add(index, handler);
	}

	public void setHandlers(List<MessageHandler> handlers) {
		this.handlers.clear();
		this.handlers.addAll(handlers);
	}

	/**
	 * Get an immutable list of handlers
	 */
	public List<MessageHandler> getHandlers() {
		return Collections.unmodifiableList(handlers);
	}

	public final Message<?> handle(Message<?> message) {
		for (MessageHandler next : handlers) {
			message = next.handle(message);
			if (message == null) {
				return null;
			}
		}
		return message;
	}

}
