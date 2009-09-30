/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MessageProcessingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Transformer.
 * 
 * @author Mark Fisher
 */
public class TransformerFactoryBean extends AbstractMessageHandlerFactoryBean {

	@Override
	MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		Assert.notNull(targetObject, "targetObject must not be null");
		Transformer transformer = null;
		if (targetObject instanceof Transformer) {
			transformer = (Transformer) targetObject;
		}
		else if (StringUtils.hasText(targetMethodName)) {
			MessageProcessor messageProcessor = new MethodInvokingMessageProcessor(targetObject, targetMethodName);
			transformer = new MessageProcessingTransformer(messageProcessor);
		}
		else {
			MessageProcessor messageProcessor = new MethodInvokingMessageProcessor(
					targetObject, org.springframework.integration.annotation.Transformer.class);
			transformer = new MessageProcessingTransformer(messageProcessor);
		}
		return new MessageTransformingHandler(transformer);
	}

	@Override
	MessageHandler createExpressionEvaluatingHandler(String expression) {
		MessageProcessor processor = new ExpressionEvaluatingMessageProcessor(expression);
		Transformer transformer = new MessageProcessingTransformer(processor);
		return new MessageTransformingHandler(transformer);
	}

}
