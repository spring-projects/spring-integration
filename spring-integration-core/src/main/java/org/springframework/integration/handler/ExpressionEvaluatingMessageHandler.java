/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} that evaluates
 * the provided {@link Expression} expecting a <b>void return</b>.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 *
 * @see MethodInvokingMessageHandler
 */
public class ExpressionEvaluatingMessageHandler extends AbstractMessageHandler {

	private final ExpressionEvaluatingMessageProcessor<Void> processor;

	private String componentType;

	public ExpressionEvaluatingMessageHandler(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.processor = new ExpressionEvaluatingMessageProcessor<>(expression, Void.class);
		setPrimaryExpression(expression);
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getComponentType() {
		return this.componentType;
	}

	@Override
	protected void onInit() {
		this.processor.setBeanFactory(getBeanFactory());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		this.processor.processMessage(message);
	}

}

