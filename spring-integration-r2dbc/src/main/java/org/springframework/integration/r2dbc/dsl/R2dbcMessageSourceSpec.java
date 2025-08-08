/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.r2dbc.dsl;

import java.util.function.BiFunction;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.r2dbc.inbound.R2dbcMessageSource;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * The {@link MessageSourceSpec} for the {@link R2dbcMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class R2dbcMessageSourceSpec extends MessageSourceSpec<R2dbcMessageSourceSpec, R2dbcMessageSource> {

	protected R2dbcMessageSourceSpec(R2dbcEntityOperations r2dbcEntityOperations, String query) {
		this.target = new R2dbcMessageSource(r2dbcEntityOperations, query);
	}

	protected R2dbcMessageSourceSpec(R2dbcEntityOperations r2dbcEntityOperations, Expression queryExpression) {
		this.target = new R2dbcMessageSource(r2dbcEntityOperations, queryExpression);
	}

	/**
	 * Set the expected payload type.
	 * @param payloadType the class to use.
	 * @return the spec
	 */
	public R2dbcMessageSourceSpec payloadType(Class<?> payloadType) {
		this.target.setPayloadType(payloadType);
		return this;
	}

	/**
	 * Configure an update query.
	 * @param updateSql the update query string.
	 * @return the spec
	 */
	public R2dbcMessageSourceSpec updateSql(String updateSql) {
		this.target.setUpdateSql(updateSql);
		return this;
	}

	/**
	 * Set a {@link BiFunction} which is used to bind parameters into the update query.
	 * @param bindFunction the {@link BiFunction} to use.
	 * @return the spec
	 */
	public R2dbcMessageSourceSpec bindFunction(
			BiFunction<DatabaseClient.GenericExecuteSpec, ?, DatabaseClient.GenericExecuteSpec> bindFunction) {

		this.target.setBindFunction(bindFunction);
		return this;
	}

	/**
	 * The flag to manage which find* method to invoke on {@link R2dbcEntityOperations}.
	 * @param expectSingleResult true if a single result is expected.
	 * @return the spec
	 */
	public R2dbcMessageSourceSpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

}
