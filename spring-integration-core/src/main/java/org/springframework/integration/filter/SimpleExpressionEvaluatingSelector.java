/*
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A {@link org.springframework.integration.core.MessageSelector} implementation that
 * evaluates a simple SpEL expression - relies on the
 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext}.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see ExpressionEvaluatingSelector
 */
public class SimpleExpressionEvaluatingSelector extends AbstractMessageProcessingSelector {

	private final String expressionString;

	@SuppressWarnings("this-escape")
	public SimpleExpressionEvaluatingSelector(String expressionString) {
		super(new ExpressionEvaluatingMessageProcessor<>(expressionString, Boolean.class));
		((ExpressionEvaluatingMessageProcessor<?>) getMessageProcessor()).setSimpleEvaluationContext(true);
		this.expressionString = expressionString;
	}

	@SuppressWarnings("this-escape")
	public SimpleExpressionEvaluatingSelector(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<>(expression, Boolean.class));
		((ExpressionEvaluatingMessageProcessor<?>) getMessageProcessor()).setSimpleEvaluationContext(true);
		this.expressionString = expression.getExpressionString();
	}

	public String getExpressionString() {
		return this.expressionString;
	}

	@Override
	public String toString() {
		return "SimpleExpressionEvaluatingSelector for: [" + this.expressionString + "]";
	}

}
