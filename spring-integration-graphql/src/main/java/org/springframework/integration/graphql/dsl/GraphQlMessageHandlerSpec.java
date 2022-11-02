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

package org.springframework.integration.graphql.dsl;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.graphql.outbound.GraphQlMessageHandler;
import org.springframework.messaging.Message;

/**
 * The {@link MessageHandlerSpec} for {@link GraphQlMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class GraphQlMessageHandlerSpec extends MessageHandlerSpec<GraphQlMessageHandlerSpec, GraphQlMessageHandler> {

	protected GraphQlMessageHandlerSpec(ExecutionGraphQlService graphQlService) {
		this.target = new GraphQlMessageHandler(graphQlService);
	}

	public GraphQlMessageHandlerSpec operation(String operation) {
		this.target.setOperation(operation);
		return this;
	}

	public GraphQlMessageHandlerSpec operation(Function<Message<?>, String> operationFunction) {
		return operationExpression(new FunctionExpression<>(operationFunction));
	}

	public GraphQlMessageHandlerSpec operationExpression(String operationExpression) {
		return operationExpression(PARSER.parseExpression(operationExpression));
	}

	public GraphQlMessageHandlerSpec operationExpression(Expression operationExpression) {
		this.target.setOperationExpression(operationExpression);
		return this;
	}

	public GraphQlMessageHandlerSpec operationName(String operationName) {
		this.target.setOperationName(operationName);
		return this;
	}

	public GraphQlMessageHandlerSpec operationName(Function<Message<?>, String> operationNameFunction) {
		return operationNameExpression(new FunctionExpression<>(operationNameFunction));
	}

	public GraphQlMessageHandlerSpec operationNameExpression(String operationNameExpression) {
		return operationNameExpression(PARSER.parseExpression(operationNameExpression));
	}

	public GraphQlMessageHandlerSpec operationNameExpression(Expression operationNameExpression) {
		this.target.setOperationNameExpression(operationNameExpression);
		return this;
	}

	public GraphQlMessageHandlerSpec variables(Function<Message<?>, Map<String, Object>> variablesFunction) {
		return variablesExpression(new FunctionExpression<>(variablesFunction));
	}

	public GraphQlMessageHandlerSpec variablesExpression(String variablesExpression) {
		return variablesExpression(PARSER.parseExpression(variablesExpression));
	}

	public GraphQlMessageHandlerSpec variablesExpression(Expression variablesExpression) {
		this.target.setVariablesExpression(variablesExpression);
		return this;
	}

	public GraphQlMessageHandlerSpec locale(Locale locale) {
		this.target.setLocale(locale);
		return this;
	}

	public GraphQlMessageHandlerSpec executionId(Function<Message<?>, String> executionIdExpression) {
		return executionIdExpression(new FunctionExpression<>(executionIdExpression));
	}

	public GraphQlMessageHandlerSpec executionIdExpression(String executionIdExpression) {
		return executionIdExpression(PARSER.parseExpression(executionIdExpression));
	}

	public GraphQlMessageHandlerSpec executionIdExpression(Expression executionIdExpression) {
		this.target.setExecutionIdExpression(executionIdExpression);
		return this;
	}

}
