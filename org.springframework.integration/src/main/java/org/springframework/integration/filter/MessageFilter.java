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

package org.springframework.integration.filter;

import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * Message Handler that delegates to a {@link MessageSelector}. If and only if
 * the selector {@link MessageSelector#accept(Message) accepts} the Message, it
 * will be passed to this filter's output channel. Otherwise the message will
 * either be silently dropped (the default) or will trigger the throwing of a
 * {@link MessageRejectedException} depending on the value of its
 * {@link #throwExceptionOnRejection} property.
 * 
 * @author Mark Fisher
 */
public class MessageFilter extends AbstractReplyProducingMessageHandler {

	private final MessageSelector selector;

	private volatile boolean throwExceptionOnRejection;


	/**
	 * Create a MessageFilter that will delegate to the given
	 * {@link MessageSelector}.
	 */
	public MessageFilter(MessageSelector selector) {
		Assert.notNull(selector, "selector must not be null");
		this.selector = selector;
	}


	/**
	 * Specify whether this filter should throw a 
	 * {@link MessageRejectedException} when its selector does not accept a
	 * Message. The default value is <code>false</code> meaning that rejected
	 * Messages will be quietly dropped.
	 */
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	@Override
	protected void handleRequestMessage(Message<?> message, ReplyMessageHolder replyHolder) {
		if (this.selector.accept(message)) {
			replyHolder.set(message);
		}
		else if (this.throwExceptionOnRejection) {
			throw new MessageRejectedException(message);
		}
	}

}
