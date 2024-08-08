/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.filter;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A {@link org.springframework.integration.core.MessageSelector} implementation that
 * evaluates a simple SpEL expression - relies on the
 * {@link org.springframework.expression.spel.support.SimpleEvaluationContext}.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see ExpressionEvaluatingSelector
 */
public class SimpleExpressionEvaluatingSelector extends AbstractMessageProcessingSelector {

	private final String expressionString;

	public SimpleExpressionEvaluatingSelector(String expressionString) {
		super(new ExpressionEvaluatingMessageProcessor<>(expressionString, Boolean.class));
		((ExpressionEvaluatingMessageProcessor<?>) getMessageProcessor()).setSimpleEvaluationContext(true);
		this.expressionString = expressionString;
	}

	public SimpleExpressionEvaluatingSelector(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<>(expression, Boolean.class));
		((ExpressionEvaluatingMessageProcessor<?>) getMessageProcessor()).setSimpleEvaluationContext(true);
		this.expressionString = expression.getExpressionString();
	}

	public String getExpressionString() {
		return this.expressionString;
	}

	@Override
	public String toString() {
		return "SimpleExpressionEvaluatingSelector for: [" + this.expressionString + "]";
	}

}
