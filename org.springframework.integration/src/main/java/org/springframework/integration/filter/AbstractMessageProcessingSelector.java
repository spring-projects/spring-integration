/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * A base class for {@link MessageSelector} implementations that delegate to
 * a {@link MessageProcessor}.
 * 
 * @author Mark Fisher
 */
abstract class AbstractMessageProcessingSelector implements MessageSelector {

	private final MessageProcessor messageProcessor;


	public AbstractMessageProcessingSelector(MessageProcessor messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}


	protected void setConversionService(ConversionService conversionService) {
		if (this.messageProcessor instanceof AbstractMessageProcessor) {
			((AbstractMessageProcessor) this.messageProcessor).setConversionService(conversionService);
		}
	}

	public final boolean accept(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		Assert.notNull(result, "result must not be null");
		Assert.isAssignable(Boolean.class, result.getClass(), "a boolean result is required");
		return (Boolean) result;
	}

}
