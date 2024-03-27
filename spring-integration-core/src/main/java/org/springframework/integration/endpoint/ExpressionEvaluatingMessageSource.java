/*
 * Copyright 2002-2024 the original author or authors.
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
