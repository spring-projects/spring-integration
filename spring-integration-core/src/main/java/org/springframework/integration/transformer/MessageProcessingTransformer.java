/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.AbstractMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * {@link Transformer) implementation that delegate to a {@link MessageProcessor}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessageProcessingTransformer implements Transformer, BeanFactoryAware {

	private final MessageProcessor<?> messageProcessor;


	public MessageProcessingTransformer(MessageProcessor<?> messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}


	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.messageProcessor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(beanFactory);
		}
		if (this.messageProcessor instanceof AbstractMessageProcessor) {
			ConversionService conversionService = IntegrationContextUtils.getConversionService(beanFactory);
			if (conversionService != null) {
				((AbstractMessageProcessor<?>) this.messageProcessor).setConversionService(conversionService);
			}
		}
	}

	public final Message<?> transform(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		if (result == null) {
			return null;
		}
		if (result instanceof Message<?>) {
			return (Message<?>) result;
		}
		return MessageBuilder.withPayload(result).copyHeaders(message.getHeaders()).build();
	}

}
