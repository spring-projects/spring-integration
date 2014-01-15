/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A reply-producing {@link MessageHandler} that delegates to a
 * {@link Transformer} instance to modify the received {@link Message}
 * and sends the result to its output channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MessageTransformingHandler extends AbstractReplyProducingMessageHandler {

	private final Transformer transformer;


	/**
	 * Create a {@link MessageTransformingHandler} instance that delegates to
	 * the provided {@link Transformer}.
	 *
	 * @param transformer The transformer.
	 */
	public MessageTransformingHandler(Transformer transformer) {
		Assert.notNull(transformer, "transformer must not be null");
		this.transformer = transformer;
		this.setRequiresReply(true);
	}


	@Override
	public String getComponentType() {
		return (this.transformer instanceof NamedComponent) ?
				((NamedComponent) this.transformer).getComponentType() : "transformer";
	}

	@Override
	protected void doInit() {
		if (this.getBeanFactory() != null && this.transformer instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.transformer).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		try {
			return transformer.transform(message);
		}
		catch (Exception e) {
			if (e instanceof MessageTransformationException) {
				throw (MessageTransformationException) e;
			}
			throw new MessageTransformationException(message, e);
		}
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

}
