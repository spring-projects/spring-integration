/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.expression.Expression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.util.Assert;

/**
 * A {@link ReleaseStrategy} that evaluates an expression.
 *
 * @author Dave Syer
 * @author Artem Bilan
 */
public class ExpressionEvaluatingReleaseStrategy extends AbstractExpressionEvaluator implements ReleaseStrategy {

	private final Expression expression;

	public ExpressionEvaluatingReleaseStrategy(String expression) {
		Assert.hasText(expression, "'expression' must not be empty");
		this.expression = EXPRESSION_PARSER.parseExpression(expression);
	}

	public ExpressionEvaluatingReleaseStrategy(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.expression = expression;
	}

	/**
	 * Evaluate the expression provided on the {@link MessageGroup}
	 * and return the result (must be boolean).
	 */
	public boolean canRelease(MessageGroup messages) {
		return Boolean.TRUE.equals(evaluateExpression(this.expression, messages, Boolean.class));
	}

}
