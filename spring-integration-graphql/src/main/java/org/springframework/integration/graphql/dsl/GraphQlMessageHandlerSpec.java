/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
