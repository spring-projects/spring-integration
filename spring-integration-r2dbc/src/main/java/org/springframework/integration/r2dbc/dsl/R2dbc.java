/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.r2dbc.dsl;

import java.util.function.Function;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.r2dbc.inbound.R2dbcMessageSource;

/**
 * Java DSL Factory class for R2DBC components.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public final class R2dbc {

	/**
	 * Create an instance of {@link R2dbcMessageSourceSpec} for the provided {@link R2dbcEntityOperations}
	 * and query string.
	 * @param r2dbcEntityOperations the {@link R2dbcEntityOperations} to use.
	 * @param query the query to execute.
	 * @return the spec.
	 */
	public static R2dbcMessageSourceSpec inboundChannelAdapter(R2dbcEntityOperations r2dbcEntityOperations,
			String query) {

		return new R2dbcMessageSourceSpec(r2dbcEntityOperations, query);
	}

	/**
	 * Create an instance of {@link R2dbcMessageSourceSpec} for the provided {@link R2dbcEntityOperations}
	 * and function to create a {@link StatementMapper.SelectSpec} instance.
	 * @param r2dbcEntityOperations the {@link R2dbcEntityOperations} to use.
	 * @param selectFunction the expression to evaluate a query for execution.
	 * @return the spec.
	 */
	public static R2dbcMessageSourceSpec inboundChannelAdapter(R2dbcEntityOperations r2dbcEntityOperations,
			Function<R2dbcMessageSource.SelectCreator, StatementMapper.SelectSpec> selectFunction) {

		return inboundChannelAdapter(r2dbcEntityOperations, new FunctionExpression<>(selectFunction));
	}

	/**
	 * Create an instance of {@link R2dbcMessageSourceSpec} for the provided {@link R2dbcEntityOperations}
	 * and SpEL expression for query.
	 * @param r2dbcEntityOperations the {@link R2dbcEntityOperations} to use.
	 * @param queryExpression the expression to evaluate a query for execution.
	 * @return the spec.
	 */
	public static R2dbcMessageSourceSpec inboundChannelAdapter(R2dbcEntityOperations r2dbcEntityOperations,
			Expression queryExpression) {

		return new R2dbcMessageSourceSpec(r2dbcEntityOperations, queryExpression);
	}

	/**
	 * Create an instance of {@link R2dbcMessageHandlerSpec} for the provided {@link R2dbcEntityOperations}.
	 * @param r2dbcEntityOperations the {@link R2dbcEntityOperations} to use.
	 * @return the spec.
	 */
	public static R2dbcMessageHandlerSpec outboundChannelAdapter(R2dbcEntityOperations r2dbcEntityOperations) {
		return new R2dbcMessageHandlerSpec(r2dbcEntityOperations);
	}

	private R2dbc() {
	}

}
