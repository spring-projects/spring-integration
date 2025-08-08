/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.expression.Expression;
import org.springframework.integration.context.ExpressionCapable;
import org.springframework.util.Assert;

/**
 * @param <T> the expected payload type.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
public class ExpressionEvaluatingMessageSource<T> extends AbstractMessageSource<T> implements ExpressionCapable {

	private final Expression expression;

	private final Class<T> expectedType;

	public ExpressionEvaluatingMessageSource(Expression expression, Class<T> expectedType) {
		Assert.notNull(expression, "expression must not be null");
		this.expression = expression;
		this.expectedType = expectedType;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	public T doReceive() {
		return this.evaluateExpression(this.expression, this.expectedType);
	}

	@Override
	public Expression getExpression() {
		return this.expression;
	}

}
