/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Collection;

import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base class for aggregators that evaluates a SpEL expression with the message list as the root object within the
 * evaluation context.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @since 2.0
 */
public class ExpressionEvaluatingMessageListProcessor extends AbstractExpressionEvaluator
		implements MessageListProcessor {

	private final Expression expression;

	private volatile Class<?> expectedType = null;

	/**
	 * Construct {@link ExpressionEvaluatingMessageListProcessor} for the provided
	 * SpEL expression and expected result type.
	 * @param expression a SpEL expression to evaluate in {@link #process(Collection)}.
	 * @param expectedType an expected result type.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageListProcessor(String expression, Class<?> expectedType) {
		this(expression);
		this.expectedType = expectedType;
	}

	/**
	 * Construct {@link ExpressionEvaluatingMessageListProcessor} for the provided
	 * SpEL expression and expected result type.
	 * @param expression a SpEL expression to evaluate in {@link #process(Collection)}.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageListProcessor(String expression) {
		try {
			this.expression = EXPRESSION_PARSER.parseExpression(expression);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
	}

	/**
	 * Construct {@link ExpressionEvaluatingMessageListProcessor} for the provided
	 * expression and expected result type.
	 * @param expression an expression to evaluate in {@link #process(Collection)}.
	 * @param expectedType an expected result type.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageListProcessor(Expression expression, Class<?> expectedType) {
		this(expression);
		this.expectedType = expectedType;
	}

	/**
	 * Construct {@link ExpressionEvaluatingMessageListProcessor} for the provided expression.
	 * @param expression an expression to evaluate in {@link #process(Collection)}.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageListProcessor(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null.");
		this.expression = expression;
	}

	/**
	 * Set the result type expected from evaluation of the expression.
	 * @param expectedType The expected type.
	 */
	public void setExpectedType(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * Processes the Message by evaluating the expression with that Message as the root object. The expression
	 * evaluation result Object will be returned.
	 */
	@Override
	public Object process(Collection<? extends Message<?>> messages) {
		return this.evaluateExpression(this.expression, messages, this.expectedType);
	}

}
