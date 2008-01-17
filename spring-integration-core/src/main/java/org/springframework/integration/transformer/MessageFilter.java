/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * Handler for deciding whether to pass a message. Implements
 * {@link MessageHandler} and simply delegates to a {@link MessageSelector}.
 * 
 * @author Mark Fisher
 */
public class MessageFilter implements MessageHandler {

	private MessageSelector selector;


	public MessageFilter(MessageSelector selector) {
		this.selector = selector;
	}

	public Message<?> handle(Message<?> message) {
		if (this.selector.accept(message)) {
			return message;
		}
		return null;
	}

}
