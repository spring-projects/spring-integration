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

package org.springframework.integration.endpoint;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSupport} sub-class that provides {@linkplain #payloadExpression}
 * evaluation with result as a payload for Message to send.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 *
 */
public abstract class ExpressionMessageProducerSupport extends MessageProducerSupport {

	private volatile Expression payloadExpression;

	private volatile EvaluationContext evaluationContext;

	/**
	 * @param payloadExpression the expression to use.
	 * @since 4.3
	 */
	public void setPayloadExpression(Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	/**
	 * @param payloadExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setPayloadExpressionString(String payloadExpression) {
		Assert.hasText(payloadExpression, "'payloadExpression' must not be empty");
		this.payloadExpression = EXPRESSION_PARSER.parseExpression(payloadExpression);
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		}
	}

	protected Object evaluatePayloadExpression(Object payload) {
		Object evaluationResult = payload;
		if (this.payloadExpression != null) {
			evaluationResult = this.payloadExpression.getValue(this.evaluationContext, payload);
		}
		return evaluationResult;
	}

}
