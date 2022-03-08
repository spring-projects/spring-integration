/*
 * Copyright 2022 the original author or authors.
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

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.RequestInput;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An {@link AbstractReplyProducingMessageHandler} capable of fielding
 * GraphQL Query, Mutation and Subscription requests.
 *
 * @author Daniel Frey
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class GraphQlMessageHandler extends AbstractReplyProducingMessageHandler {

	private final GraphQlService graphQlService;

	private StandardEvaluationContext evaluationContext;

	private Expression operationExpression;

	private Expression operationNameExpression = new SupplierExpression<>(() -> null);

	private Expression variablesExpression = new SupplierExpression<>(() -> null);

	@Nullable
	private Locale locale;

	private Expression executionIdExpression =
			new FunctionExpression<Message<?>>(message -> message.getHeaders().getId());

	public GraphQlMessageHandler(final GraphQlService graphQlService) {
		Assert.notNull(graphQlService, "'graphQlService' must not be null");
		this.graphQlService = graphQlService;
		setAsync(true);
	}

	/**
	 * Specify a GraphQL Operation.
	 * @param operation the GraphQL operation to use.
	 */
	public void setOperation(String operation) {
		Assert.hasText(operation, "'operation' must not be empty");
		setOperationExpression(new LiteralExpression(operation));
	}

	/**
	 * Specify a SpEL expression to evaluate a GraphQL Operation
	 * @param operationExpression the expression to evaluate a GraphQL Operation.
	 */
	public void setOperationExpression(Expression operationExpression) {
		Assert.notNull(operationExpression, "'queryExpression' must not be null");
		this.operationExpression = operationExpression;
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
	 * Set a SpEL expression to evaluate Variables for GraphQL Operation to execute.
	 * @param variablesExpression the expression to use.
	 */
	public void setVariablesExpression(Expression variablesExpression) {
		Assert.notNull(variablesExpression, "'variablesExpression' must not be null");
		this.variablesExpression = variablesExpression;
	}

	/**
	 * Set a Locale for GraphQL Operation to execute.
	 * @param locale the locale to use.
	 */
	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	/**
	 * Set a SpEL expression to evaluate Execution Id for GraphQL Operation Request to execute.
	 * @param executionIdExpression the executionIdExpression to use.
	 */
	public void setExecutionIdExpression(Expression executionIdExpression) {
		Assert.notNull(executionIdExpression, "'executionIdExpression' must not be null");
		this.executionIdExpression = executionIdExpression;
	}

	@Override
	protected final void doInit() {
		BeanFactory beanFactory = getBeanFactory();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		RequestInput requestInput;

		if (requestMessage.getPayload() instanceof RequestInput) {
			requestInput = (RequestInput) requestMessage.getPayload();
		}
		else {
			Assert.notNull(this.operationExpression, "'operationExpression' must not be null");
			String query = evaluateOperationExpression(requestMessage);
			String operationName = evaluateOperationNameExpression(requestMessage);
			Map<String, Object> variables = evaluateVariablesExpression(requestMessage);
			String id = evaluateExecutionIdExpression(requestMessage);
			requestInput = new RequestInput(query, operationName, variables, id, this.locale);
		}

		return this.graphQlService.execute(requestInput);

	}

	private String evaluateOperationExpression(Message<?> message) {
		String operation = this.operationExpression.getValue(this.evaluationContext, message, String.class);
		Assert.notNull(operation, "'operationExpression' must not evaluate to null");
		return operation;
	}

	private String evaluateOperationNameExpression(Message<?> message) {
		return this.operationNameExpression.getValue(this.evaluationContext, message, String.class);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> evaluateVariablesExpression(Message<?> message) {
		return this.variablesExpression.getValue(this.evaluationContext, message, Map.class);
	}

	private String evaluateExecutionIdExpression(Message<?> message) {
		return this.executionIdExpression.getValue(this.evaluationContext, message, String.class);
	}

}
