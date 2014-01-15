/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.expression.ParseException;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageProcessor} implementation that evaluates a SpEL expression
 * with the Message itself as the root object within the evaluation context.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessor<T> extends AbstractMessageProcessor<T> {

	private final Expression expression;

	private final Class<T> expectedType;


	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression.
	 *
	 * @param expression The expression.
	 */
	public ExpressionEvaluatingMessageProcessor(Expression expression) {
		this(expression, null);
	}


	/**
	 * Create an {@link ExpressionEvaluatingMessageProcessor} for the given expression
	 * and expected type for its evaluation result.
	 *
	 * @param expression The expression.
	 * @param expectedType The expected type.
	 */
	public ExpressionEvaluatingMessageProcessor(Expression expression, Class<T> expectedType) {
		Assert.notNull(expression, "The expression must not be null");
		try {
			this.expression = expression;
			this.expectedType = expectedType;
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
	}


	/**
	 * Processes the Message by evaluating the expression with that Message as the
	 * root object. The expression evaluation result Object will be returned.
	 *
	 * @param message The message.
	 * @return The result of processing the message.
	 */
	@Override
	public T processMessage(Message<?> message) {
		return this.evaluateExpression(this.expression, message, this.expectedType);
	}

}
