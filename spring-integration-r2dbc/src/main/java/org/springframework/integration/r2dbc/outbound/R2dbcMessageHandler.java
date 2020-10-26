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

package org.springframework.integration.r2dbc.outbound;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;


/**
 * Implementation of {@link org.springframework.messaging.ReactiveMessageHandler} which writes
 * Message payload into a Relational Database, using reactive r2dbc support.
 *
 * @author Rohan Mukesh
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class R2dbcMessageHandler extends AbstractReactiveMessageHandler {

	private final R2dbcEntityOperations r2dbcEntityOperations;

	private final StatementMapper statementMapper;

	private StandardEvaluationContext evaluationContext;

	private Expression queryTypeExpression = new ValueExpression<>(Type.INSERT);

	@Nullable
	private Expression tableNameExpression;

	@Nullable
	private Expression valuesExpression;

	@Nullable
	private Expression criteriaExpression;

	private volatile boolean initialized = false;

	/**
	 * Construct this instance using a fully created and initialized instance of provided
	 * {@link R2dbcEntityOperations}
	 * @param r2dbcEntityOperations The R2dbcEntityOperations implementation.
	 */
	@SuppressWarnings("deprecation")
	public R2dbcMessageHandler(R2dbcEntityOperations r2dbcEntityOperations) {
		Assert.notNull(r2dbcEntityOperations, "'r2dbcEntityOperations' must not be null");
		this.r2dbcEntityOperations = r2dbcEntityOperations;
		this.statementMapper = this.r2dbcEntityOperations.getDataAccessStrategy().getStatementMapper();
	}


	/**
	 * Set a {@link R2dbcMessageHandler.Type} for query to execute.
	 * @param type the {@link R2dbcMessageHandler.Type} to use.
	 */
	public void setQueryType(R2dbcMessageHandler.Type type) {
		setQueryTypeExpression(new ValueExpression<>(type));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link R2dbcMessageHandler.Type} for query to execute.
	 * @param queryTypeExpression the expression to use.
	 */
	public void setQueryTypeExpression(Expression queryTypeExpression) {
		Assert.notNull(queryTypeExpression, "'queryTypeExpression' must not be null");
		this.queryTypeExpression = queryTypeExpression;
	}

	/**
	 * Specify a table in the target database to execute the query.
	 * @param tableName the name of the table to use.
	 */
	public void setTableName(String tableName) {
		setTableNameExpression(new LiteralExpression(tableName));
	}

	/**
	 * Set a SpEL expression to evaluate a table name at runtime against request message.
	 * @param tableNameExpression the expression to use.
	 */
	public void setTableNameExpression(Expression tableNameExpression) {
		this.tableNameExpression = tableNameExpression;
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Map} for name-value pairs to bind as parameters
	 * into a query.
	 * @param valuesExpression the expression to use.
	 */
	public void setValuesExpression(Expression valuesExpression) {
		this.valuesExpression = valuesExpression;
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Criteria} for query to execute.
	 * @param criteriaExpression the expression to use.
	 */
	public void setCriteriaExpression(Expression criteriaExpression) {
		this.criteriaExpression = criteriaExpression;
	}


	@Override
	public String getComponentType() {
		return "r2dbc:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			//Register R2dbc criteria API package so FQCN can be avoided in query-expression.
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.relational.core.query");
		}
		this.initialized = true;

	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		Assert.isTrue(this.initialized, "The instance is not yet initialized. Invoke its afterPropertiesSet() method");
		return Mono.fromSupplier(() -> this.queryTypeExpression.getValue(this.evaluationContext, message, Type.class))
				.flatMap(mode -> {
					switch (mode) {
						case INSERT:
							return handleInsert(message);
						case UPDATE:
							return handleUpdate(message);
						case DELETE:
							return handleDelete(message);
						default:
							return Mono.error(new IllegalArgumentException());
					}
				}).then();
	}


	private Mono<Void> handleDelete(Message<?> message) {
		if (this.tableNameExpression != null) {
			String tableName = evaluateTableNameExpression(message);
			Criteria criteria = evaluateCriteriaExpression(message);
			StatementMapper.DeleteSpec deleteSpec =
					this.statementMapper.createDelete(tableName)
							.withCriteria(criteria);
			PreparedOperation<?> operation = this.statementMapper.getMappedObject(deleteSpec);
			return this.r2dbcEntityOperations.getDatabaseClient()
					.sql(operation)
					.then();
		}
		else {
			return this.r2dbcEntityOperations.delete(message.getPayload())
					.then();
		}
	}

	private Mono<Void> handleUpdate(Message<?> message) {
		if (this.tableNameExpression != null) {
			String tableName = evaluateTableNameExpression(message);
			Map<String, Object> values = evaluateValuesExpression(message);
			Map<SqlIdentifier, Object> updateMap = transformIntoSqlIdentifierMap(values);
			Criteria criteria = evaluateCriteriaExpression(message);

			StatementMapper.UpdateSpec updateSpec =
					this.statementMapper.createUpdate(tableName, Update.from(updateMap))
							.withCriteria(criteria);
			PreparedOperation<?> operation = this.statementMapper.getMappedObject(updateSpec);
			return this.r2dbcEntityOperations.getDatabaseClient()
					.sql(operation)
					.then();
		}
		else {
			return this.r2dbcEntityOperations.update(message.getPayload())
					.then();
		}
	}

	private Map<SqlIdentifier, Object> transformIntoSqlIdentifierMap(Map<String, Object> values) {
		Map<SqlIdentifier, Object> sqlIdentifierObjectMap = new HashMap<>();
		values.forEach((k, v) -> sqlIdentifierObjectMap.put(SqlIdentifier.unquoted(k), v));
		return sqlIdentifierObjectMap;
	}

	private Mono<Void> handleInsert(Message<?> message) {
		if (this.tableNameExpression != null) {
			String tableName = evaluateTableNameExpression(message);
			Map<String, Object> values = evaluateValuesExpression(message);

			StatementMapper.InsertSpec insertSpec = this.statementMapper.createInsert(tableName);

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				insertSpec = insertSpec.withColumn(entry.getKey(),
						Parameter.fromOrEmpty(entry.getValue(), Object.class));
			}

			PreparedOperation<?> operation = this.statementMapper.getMappedObject(insertSpec);
			return this.r2dbcEntityOperations.getDatabaseClient()
					.sql(operation)
					.then();
		}
		else {
			return this.r2dbcEntityOperations.insert(message.getPayload())
					.then();
		}
	}

	private String evaluateTableNameExpression(Message<?> message) {
		String tableName =
				this.tableNameExpression.getValue(this.evaluationContext, message, String.class); // NOSONAR
		Assert.notNull(tableName, "'tableNameExpression' must not evaluate to null");
		return tableName;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> evaluateValuesExpression(Message<?> message) {
		Assert.notNull(this.valuesExpression,
				"'this.valuesExpression' must not be null when 'tableNameExpression' mode is used");
		Map<String, Object> fieldValues =
				(Map<String, Object>) this.valuesExpression.getValue(this.evaluationContext, message, Map.class);
		Assert.notNull(fieldValues, "'valuesExpression' must not evaluate to null");
		return fieldValues;
	}

	private Criteria evaluateCriteriaExpression(Message<?> message) {
		Assert.notNull(this.criteriaExpression,
				"'this.criteriaExpression' must not be null when 'tableNameExpression' mode is used");
		Criteria criteria = this.criteriaExpression.getValue(this.evaluationContext, message, Criteria.class);
		Assert.notNull(criteria, "'criteriaExpression' must not evaluate to null");
		return criteria;
	}


	/**
	 * /**
	 * The mode for the {@link R2dbcMessageHandler}.
	 */
	public enum Type {

		/**
		 * Set a {@link R2dbcMessageHandler} into an {@code insert} mode.
		 */
		INSERT,

		/**
		 * Set a {@link R2dbcMessageHandler} into an {@code update} mode.
		 */
		UPDATE,

		/**
		 * Set a {@link R2dbcMessageHandler} into a {@code delete} mode.
		 */
		DELETE,

	}

}
