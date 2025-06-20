/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Strategy interface for dispatching messages to handlers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public interface MessageDispatcher {

	/**
	 * Add a message handler.
	 * @param handler the handler.
	 * @return true if successfully added.
	 */
	boolean addHandler(MessageHandler handler);

	/**
	 * Remove a message handler.
	 * @param handler the handler.
	 * @return true of successfully removed.
	 */
	boolean removeHandler(MessageHandler handler);

	/**
	 * Dispatch the message.
	 * @param message the message.
	 * @return true if dispatched.
	 */
	boolean dispatch(Message<?> message);

	/**
	 * Return the current handler count.
	 * @return the handler count.
	 */
	int getHandlerCount();

}
