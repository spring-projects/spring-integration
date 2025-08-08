/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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

