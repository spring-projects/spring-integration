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

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A message handler implementation that intercepts calls to another handler.
 * 
 * @author Mark Fisher
 */
public abstract class InterceptingMessageHandler implements MessageHandler {

	private MessageHandler target;


	public InterceptingMessageHandler(MessageHandler target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}

	public final Message<?> handle(Message<?> message) {
		return handle(message, this.target);
	}


	/**
	 * The handler method for subclasses to implement.
	 * 
	 * @param message the message to handle
	 * @param target the intercepted handler
	 * @return a reply message or null
	 */
	public abstract Message<?> handle(Message<?> message, MessageHandler target);

}
