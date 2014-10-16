/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.routingslip;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class ExpressionEvaluationRoutingSlipRouteStrategy
		implements RoutingSlipRouteStrategy, IntegrationEvaluationContextAware {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private final Expression expression;

	private EvaluationContext evaluationContext;

	public ExpressionEvaluationRoutingSlipRouteStrategy(String expression) {
		this(PARSER.parseExpression(expression));
	}

	public ExpressionEvaluationRoutingSlipRouteStrategy(Expression expression) {
		this.expression = expression;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public String getNextPath(Message<?> requestMessage, Object reply) {
		return this.expression.getValue(this.evaluationContext, new RequestAndReply(requestMessage, reply), String.class);
	}

	public static class RequestAndReply {

		private final Message<?> request;

		private final Object reply;


		RequestAndReply(Message<?> request, Object reply) {
			this.request = request;
			this.reply = reply;
		}

		public Message<?> getRequest() {
			return request;
		}

		public Object getReply() {
			return reply;
		}

	}

}
