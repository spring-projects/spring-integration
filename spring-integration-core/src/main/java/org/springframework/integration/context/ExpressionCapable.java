/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.context;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;

/**
 * Components that implement this interface are capable of supporting a primary
 * SpEL expression as part of their configuration.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface ExpressionCapable {

	/**
	 * Return the primary SpEL expression if this component is expression-based.
	 * @return the expression as a String.
	 */
	@Nullable
	Expression getExpression();

}
