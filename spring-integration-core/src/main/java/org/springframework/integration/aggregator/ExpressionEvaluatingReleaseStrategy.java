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
