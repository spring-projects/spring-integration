/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.r2dbc.inbound;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.r2dbc.core.ColumnMapRowMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Mono;


/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of query. When {@code expectSingleResult} is false (default), the R2DBC
 * query is executed returning a {@link reactor.core.publisher.Flux}.
 * The returned {@link reactor.core.publisher.Flux} will be used as the payload of the
 * {@link org.springframework.messaging.Message} returned by the {@link #receive()}
 * method.
 * <p>
 * When {@code expectSingleResult} is true, the query is executed returning a {@link reactor.core.publisher.Mono}
 * for the single object returned from the query.
 *
 * @author Rohan Mukesh
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class R2dbcMessageSource extends AbstractMessageSource<Publisher<?>> {

	private final R2dbcEntityOperations r2dbcEntityOperations;

	private final DatabaseClient databaseClient;

	private final StatementMapper statementMapper;

	private final SelectCreator selectCreator = new SelectCreator();

	private final Expression queryExpression;

	private Class<?> payloadType = Map.class;

	private BiFunction<Row, RowMetadata, ?> rowMapper = ColumnMapRowMapper.INSTANCE;

	private boolean expectSingleResult = false;

	private StandardEvaluationContext evaluationContext;

	private String updateSql;

	private BiFunction<DatabaseClient.GenericExecuteSpec, Object, DatabaseClient.GenericExecuteSpec> bindFunction;

	private volatile boolean initialized = false;

	/**
	 * Create an instance with the provided {@link R2dbcEntityOperations} and SpEL expression
	 * which should resolve to a Relational 'query' string.
	 * It assumes that the {@link R2dbcEntityOperations} is fully initialized and ready to be used.
	 * The 'query' will be evaluated on every call to the {@link #receive()} method.
	 * @param r2dbcEntityOperations The reactive database client for performing database calls.
	 * @param query The query String.
	 */
	public R2dbcMessageSource(R2dbcEntityOperations r2dbcEntityOperations, String query) {
		this(r2dbcEntityOperations, new LiteralExpression(query));
	}

	/**
	 * Create an instance with the provided {@link R2dbcEntityOperations} and SpEL expression
	 * which should resolve to a query string or {@link StatementMapper.SelectSpec} instance.
	 * It assumes that the {@link R2dbcEntityOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param r2dbcEntityOperations  The reactive for performing database calls.
	 * @param queryExpression The query expression. The root object for evaluation context is a {@link SelectCreator}
	 * for delegation int the {@link StatementMapper#createSelect} fluent API.
	 */
	@SuppressWarnings("deprecation")
	public R2dbcMessageSource(R2dbcEntityOperations r2dbcEntityOperations, Expression queryExpression) {
		Assert.notNull(r2dbcEntityOperations, "'r2dbcEntityOperations' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.r2dbcEntityOperations = r2dbcEntityOperations;
		this.databaseClient = r2dbcEntityOperations.getDatabaseClient();
		this.statementMapper = r2dbcEntityOperations.getDataAccessStrategy().getStatementMapper();
		this.queryExpression = queryExpression;
	}

	/**
	 * Set the type of the entityClass which is used for the {@link EntityRowMapper}.
	 * @param payloadType The class to use.
	 */
	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType = payloadType;
	}

	/**
	 * Set an update query that will be passed to the {@link DatabaseClient#sql(String)} method.
	 * @param updateSql the update query string.
	 */
	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	/**
	 * Set a {@link BiFunction} which is used to bind parameters into the update query.
	 * @param bindFunction the {@link BiFunction} to use.
	 */
	@SuppressWarnings("unchecked")
	public void setBindFunction(
			BiFunction<DatabaseClient.GenericExecuteSpec, ?, DatabaseClient.GenericExecuteSpec> bindFunction) {

		this.bindFunction =
				(BiFunction<DatabaseClient.GenericExecuteSpec, Object, DatabaseClient.GenericExecuteSpec>) bindFunction;
	}

	/**
	 * The flag to manage which find* method to invoke on {@link R2dbcEntityOperations}.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@link DatabaseClient#sql(String)} method and will fetch all. If set
	 * to 'true'{@link #receive()} will use {@link DatabaseClient#sql(String)}
	 * and will fetch one and the payload of the returned {@link org.springframework.messaging.Message}
	 * will be the returned target Object of type
	 * identified by {@link #payloadType} instead of a List.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	@Override
	public String getComponentType() {
		return "r2dbc:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (!Map.class.isAssignableFrom(this.payloadType)) {
			this.rowMapper = new EntityRowMapper<>(this.payloadType, this.r2dbcEntityOperations.getConverter());
		}
		this.initialized = true;
	}

	/**
	 * Execute a query returning its results as the Message payload.
	 * The payload can be either {@link reactor.core.publisher.Flux} or
	 * {@link reactor.core.publisher.Mono} of objects of type identified by {@link #payloadType},
	 * or a single element of type identified by {@link #payloadType}
	 * based on the value of {@link #expectSingleResult} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link reactor.core.publisher.Flux}.
	 */
	@Override
	protected Object doReceive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Mono<RowsFetchSpec<?>> queryMono =
				Mono.fromSupplier(() ->
						this.queryExpression.getValue(this.evaluationContext, this.selectCreator))
						.map(this::prepareFetch);
		if (this.expectSingleResult) {
			return queryMono.flatMap(RowsFetchSpec::one)
					.flatMap(this::executeUpdate);
		}

		return queryMono.flatMapMany(RowsFetchSpec::all)
				.flatMap(this::executeUpdate);
	}

	private Mono<Object> executeUpdate(Object result) {
		if (this.updateSql != null) {
			DatabaseClient.GenericExecuteSpec genericExecuteSpec = this.databaseClient.sql(this.updateSql);
			if (this.bindFunction != null) {
				genericExecuteSpec = this.bindFunction.apply(genericExecuteSpec, result);
			}
			return genericExecuteSpec.then()
					.thenReturn(result);
		}
		return Mono.just(result);
	}

	private RowsFetchSpec<?> prepareFetch(Object queryObject) {
		Supplier<String> query = evaluateQueryObject(queryObject);
		return this.databaseClient.sql(query)
				.map(this.rowMapper);
	}

	private Supplier<String> evaluateQueryObject(Object queryObject) {
		if (queryObject instanceof String) {
			return () -> (String) queryObject;
		}
		else if (queryObject instanceof StatementMapper.SelectSpec) {
			return this.statementMapper.getMappedObject((StatementMapper.SelectSpec) queryObject);
		}
		throw new IllegalStateException("'queryExpression' must evaluate to String " +
				"or org.springframework.data.r2dbc.core.StatementMapper.SelectSpec, but not: " + queryObject);
	}

	/**
	 * An instance of this class is used as a root object for query expression
	 * to give a limited access only to the {@link StatementMapper#createSelect} fluent API.
	 */
	public class SelectCreator {

		SelectCreator() {
		}

		public StatementMapper.SelectSpec createSelect(String table) {
			return R2dbcMessageSource.this.statementMapper.createSelect(table);
		}

		public StatementMapper.SelectSpec createSelect(SqlIdentifier table) {
			return R2dbcMessageSource.this.statementMapper.createSelect(table);
		}

	}

}
