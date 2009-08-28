/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * A {@link MessageProcessor} implementation that evaluates a SpEL expression
 * with the Message itself as the root object within the evaluation context.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionEvaluatingMessageProcessor implements MessageProcessor {

	/*private final ExpressionParser parser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull |
			SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);
	*/

	private final Expression expression;


	public ExpressionEvaluatingMessageProcessor(String expression) {
		try {
			ExpressionParser parser = new SpelExpressionParser(
					SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull |
					SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);
			this.expression = parser.parseExpression(expression);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse expression.", e);
		}
	}

	public Object processMessage(Message<?> message) {
		StandardEvaluationContext context = new StandardEvaluationContext(message);
		context.addPropertyAccessor(new MapAccessor());
		try {
			return this.expression.getValue(context);
		}
		catch (EvaluationException e) {
			throw new MessageHandlingException(message, "Expression evaluation failed.", e);
		}
	}

}
