/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} that evaluates
 * the provided {@link Expression} expecting a <b>void return</b>.
 *
 * @author Artem Bilan
 * @see MethodInvokingMessageHandler
 * @since 2.1
 */
public class ExpressionEvaluatingMessageHandler extends AbstractMessageHandler {

	private volatile ExpressionEvaluatingMessageProcessor<Void> processor;

	private volatile String componentType;


	public ExpressionEvaluatingMessageHandler(Expression expression) {
		Assert.notNull(expression, "Expression must not be null");
		this.processor = new ExpressionEvaluatingMessageProcessor<Void>(
				expression, Void.class);
	}


	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getComponentType() {
		return this.componentType;
	}

	@Override
	protected void onInit() throws Exception {
		this.processor.setBeanFactory(getBeanFactory());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		this.processor.processMessage(message);
	}

}

