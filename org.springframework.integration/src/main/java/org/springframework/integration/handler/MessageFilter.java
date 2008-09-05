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

import org.springframework.integration.endpoint.AbstractInOutEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * Message Endpoint that decides whether to pass a message along to its
 * output channel. Delegates to a {@link MessageSelector}.
 * 
 * @author Mark Fisher
 */
public class MessageFilter extends AbstractInOutEndpoint {

	private MessageSelector selector;


	public MessageFilter(MessageSelector selector) {
		Assert.notNull(selector, "selector must not be null");
		this.selector = selector;
	}


	@Override
	protected Message<?> handle(Message<?> message) {
		if (this.selector.accept(message)) {
			return message;
		}
		return null;
	}

}
