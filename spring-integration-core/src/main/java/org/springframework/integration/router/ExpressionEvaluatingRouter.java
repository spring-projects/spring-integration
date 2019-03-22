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

package org.springframework.integration.router;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A Message Router implementation that evaluates the specified SpEL
 * expression. The result of evaluation will typically be a String to be
 * resolved to a channel name or a Collection (or Array) of strings.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingRouter extends AbstractMessageProcessingRouter {

	/**
	 * Construct an instance by parsing the supplied expression string.
	 * @param expressionString the expression string.
	 */
	public ExpressionEvaluatingRouter(String expressionString) {
		this(EXPRESSION_PARSER.parseExpression(expressionString));
	}

	/**
	 * Construct an instance with the supplied {@link Expression}.
	 * @param expression the expression.
	 */
	public ExpressionEvaluatingRouter(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<Object>(expression));
		setPrimaryExpression(expression);
	}

}
