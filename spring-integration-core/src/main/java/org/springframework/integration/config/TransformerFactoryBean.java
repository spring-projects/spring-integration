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

package org.springframework.integration.config;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Transformer.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 * @author Ngoc Nhan
 */
public class TransformerFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	@SuppressWarnings("this-escape")
	public TransformerFactoryBean() {
		setRequiresReply(true);
	}

	@Override
	protected MessageHandler createMethodInvokingHandler(Object targetObject, @Nullable String targetMethodName) {
		Assert.notNull(targetObject, "targetObject must not be null");
		Transformer transformer = null;
		if (targetObject instanceof Transformer castTransformer) {
			transformer = castTransformer;
		}
		else {
			this.checkForIllegalTarget(targetObject, targetMethodName);
			if (StringUtils.hasText(targetMethodName)) {
				transformer = new MethodInvokingTransformer(targetObject, targetMethodName);
			}
			else {
				transformer = new MethodInvokingTransformer(targetObject);
			}
		}
		return this.createHandler(transformer);
	}

	@Override
	protected MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		Transformer transformer = new ExpressionEvaluatingTransformer(expression);
		MessageTransformingHandler handler = this.createHandler(transformer);
		handler.setPrimaryExpression(expression);
		return handler;
	}

	protected MessageTransformingHandler createHandler(Transformer transformer) {
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		this.postProcessReplyProducer(handler);
		return handler;
	}

	/**
	 * Always returns true - any {@link AbstractMessageProducingHandler} can
	 * be used directly.
	 */
	@Override
	protected boolean canBeUsedDirect(AbstractMessageProducingHandler handler) {
		return true; // Any AMPH can be a transformer
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return MessageTransformingHandler.class;
	}

}
