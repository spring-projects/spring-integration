/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.endpoint;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.endpoint.MessageProducerSupport;

/**
 * A {@link MessageProducerSupport} sub-class that provides {@linkplain #payloadExpression}
 * evaluation with result as a payload for Message to send.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.1
 *
 */
public abstract class ExpressionMessageProducerSupport extends MessageProducerSupport {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private volatile Expression payloadExpression;

	public void setPayloadExpression(String payloadExpression) {
		if (payloadExpression == null) {
			this.payloadExpression = null;
		}
		else {
			this.payloadExpression = this.parser.parseExpression(payloadExpression);
		}
	}

	protected Object evaluatePayloadExpression(Object payload){
		Object evaluationResult = payload;
		if (payloadExpression != null) {
			evaluationResult = payloadExpression.getValue(payload);
		}
		return evaluationResult;
	}

}
