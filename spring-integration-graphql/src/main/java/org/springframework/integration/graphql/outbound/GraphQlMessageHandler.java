/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.graphql.outbound;

import java.util.Collections;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import graphql.com.google.common.base.Strings;

/**
 * A {@link org.springframework.messaging.MessageHandler} capable of fielding GraphQL Query, Mutation and Subscription requests.
 *
 * @author Daniel Frey
 * @since 6.0
 */
public class GraphQlMessageHandler extends AbstractReplyProducingMessageHandler {

	private final GraphQlService graphQlService;

	private StandardEvaluationContext evaluationContext;

	private Expression queryExpression;

	private Expression operationNameExpression = new SupplierExpression<>(() -> null);

	private Expression variablesExpression = new SupplierExpression<>(() -> Collections.emptyMap());

	public GraphQlMessageHandler(final GraphQlService graphQlService) {
		Assert.notNull(graphQlService, "'graphQlService' must not be null");

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		this.graphQlService = graphQlService;
		setAsync(true);
	}

	/**
	 * Specify a GraphQL Query.
	 * @param query the GraphQL query to use.
	 */
	public void setQuery(String query) {
		setQueryExpression(new LiteralExpression(query));
	}

	/**
	 * Specify a SpEL expression to evaluate a GraphQL Query
	 * @param queryExpression the expression to evaluate a GraphQL query.
	 */
	public void setQueryExpression(Expression queryExpression) {
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.queryExpression = queryExpression;
	}

	/**
	 * Set a GraphQL Operation Name to execute.
	 * @param operationName the GraphQL Operation Name to use.
	 */
	public void setOperationName(String operationName) {
		setOperationNameExpression(new LiteralExpression(operationName));
	}

	/**
	 * Set a SpEL expression to evaluate a GraphQL Operation Name to execute.
	 * @param operationNameExpression the expression to use.
	 */
	public void setOperationNameExpression(Expression operationNameExpression) {
		Assert.notNull(operationNameExpression, "'operationNameExpression' must not be null");
		this.operationNameExpression = operationNameExpression;
	}

	/**
	 * Specify variables for the GraphQL Query to execute.
	 * @param variables the GraphQL variables to use.
	 */
	public void setVariables(Map<String, Object> variables) {
		setVariablesExpression(new SupplierExpression<>(() -> variables));
	}

	/**
	 * Set a SpEL expression to evaluate Variables for GraphQL Query to execute.
	 * @param variablesExpression the expression to use.
	 */
	public void setVariablesExpression(Expression variablesExpression) {
		Assert.notNull(variablesExpression, "'variablesExpression' must not be null");
		this.variablesExpression = variablesExpression;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		if (requestMessage.getPayload() instanceof RequestInput) {

			return this.graphQlService
					.execute((RequestInput) requestMessage.getPayload());
		}
		else if (requestMessage.getPayload() instanceof String && !Strings.isNullOrEmpty((String) requestMessage.getPayload())) {
			String query = evaluateQueryExpression(requestMessage);
			String operationName = evaluateOperationNameExpression(requestMessage);
			Map<String, Object> variables = evaluateVariablesExpression(requestMessage);
			return this.graphQlService
					.execute(new RequestInput(query, operationName, variables));
		}
		else {
			throw new IllegalArgumentException("Message payload does not meet criteria to construct a 'org.springframework.graphql.RequestInput'");
		}
	}

	private String evaluateQueryExpression(Message<?> message) {
		String query = this.queryExpression.getValue(this.evaluationContext, message, String.class);
		Assert.notNull(query, "'queryExpression' must not evaluate to null");
		return query;
	}

	private String evaluateOperationNameExpression(Message<?> message) {
		return this.operationNameExpression.getValue(this.evaluationContext, message, String.class);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> evaluateVariablesExpression(Message<?> message) {
		return this.variablesExpression.getValue(this.evaluationContext, message, Map.class);
	}

}
