/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.util.AbstractExpressionEvaluator;

/**
 * Simple {@link AbstractExpressionEvaluator} implementation
 * to increase the visibility of protected methods.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class ParameterExpressionEvaluator extends AbstractExpressionEvaluator {

	@Override
	public EvaluationContext getEvaluationContext() {
		return super.getEvaluationContext();
	}

	@Override
	public @Nullable Object evaluateExpression(Expression expression, @Nullable Object input) {
		return super.evaluateExpression(expression, input);
	}

}
