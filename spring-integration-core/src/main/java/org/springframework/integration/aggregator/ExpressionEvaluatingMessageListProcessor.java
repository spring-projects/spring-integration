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

package org.springframework.integration.aggregator;

import java.util.Collection;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;

/**
 * A base class for aggregators that evaluates a SpEL expression with the message list as the root object within the
 * evaluation context.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class ExpressionEvaluatingMessageListProcessor extends AbstractExpressionEvaluator implements MessageListProcessor {

	private final ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final Expression expression;

	private volatile Class<?> expectedType = null;

	/**
	 * Set the result type expected from evaluation of the expression.
	 *
	 * @param expectedType The expected type.
	 */
	public void setExpectedType(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	public ExpressionEvaluatingMessageListProcessor(String expression) {
		try {
			this.expression = parser.parseExpression(expression);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
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
