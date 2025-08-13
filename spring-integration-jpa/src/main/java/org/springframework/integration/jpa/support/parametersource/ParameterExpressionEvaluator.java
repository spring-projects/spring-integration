/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jpa.support.parametersource;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.util.AbstractExpressionEvaluator;

/**
 * Simple {@link AbstractExpressionEvaluator} implementation
 * to increase the visibility of protected methods.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class ParameterExpressionEvaluator extends AbstractExpressionEvaluator {

	@Override
	public EvaluationContext getEvaluationContext() {
		return super.getEvaluationContext();
	}

	@Override
	public @Nullable Object evaluateExpression(Expression expression, Object input) {
		return super.evaluateExpression(expression, input);
	}

}
