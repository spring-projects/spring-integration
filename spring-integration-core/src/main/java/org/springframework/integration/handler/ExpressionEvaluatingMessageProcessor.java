/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageProcessor} implementation that evaluates a SpEL expression
 * with the Message itself as the root object within the evaluation context.
 *
 * @param <T> the expected payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessor<T> extends AbstractMessageProcessor<T> {

	private final Expression expression;

	private final @Nullable Class<T> expectedType;

	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression.
	 * @param expression The expression.
	 */
	public ExpressionEvaluatingMessageProcessor(Expression expression) {
		this(expression, null);
	}

	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression
	 * and expected type for its evaluation result.
	 * @param expression The expression.
	 * @param expectedType The expected type.
	 */
	public ExpressionEvaluatingMessageProcessor(Expression expression, @Nullable Class<T> expectedType) {
		Assert.notNull(expression, "The expression must not be null");
		this.expression = expression;
		this.expectedType = expectedType;
	}

	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression.
	 * @param expression a SpEL expression to evaluate.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageProcessor(String expression) {
		this(expression, null);
	}

	/**
	 * Construct {@link ExpressionEvaluatingMessageProcessor} for the provided
	 * SpEL expression and expected result type.
	 * @param expression a SpEL expression to evaluate.
	 * @param expectedType the expected result type.
	 * @since 5.0
	 */
	public ExpressionEvaluatingMessageProcessor(String expression, @Nullable Class<T> expectedType) {
		try {
			this.expression = EXPRESSION_PARSER.parseExpression(expression);
			this.expectedType = expectedType;
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
	}

	/**
	 * Processes the Message by evaluating the expression with that Message as the
	 * root object. The expression evaluation result Object will be returned.
	 * @param message The message.
	 * @return The result of processing the message.
	 */
	@Override
	public @Nullable T processMessage(Message<?> message) {
		return evaluateExpression(this.expression, message, this.expectedType);
	}

	@Override
	public String toString() {
		return "ExpressionEvaluatingMessageProcessor for: [" + this.expression.getExpressionString() + "]";
	}

}
