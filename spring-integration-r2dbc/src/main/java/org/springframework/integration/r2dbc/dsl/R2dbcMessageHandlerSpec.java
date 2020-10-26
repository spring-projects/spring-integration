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

package org.springframework.integration.r2dbc.dsl;

import java.util.Map;
import java.util.function.Function;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ReactiveMessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.r2dbc.outbound.R2dbcMessageHandler;
import org.springframework.messaging.Message;

/**
 * The {@link ReactiveMessageHandlerSpec} for the {@link R2dbcMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class R2dbcMessageHandlerSpec extends ReactiveMessageHandlerSpec<R2dbcMessageHandlerSpec, R2dbcMessageHandler> {

	protected R2dbcMessageHandlerSpec(R2dbcEntityOperations r2dbcEntityOperations) {
		super(new R2dbcMessageHandler(r2dbcEntityOperations));
	}

	/**
	 * Set a {@link R2dbcMessageHandler.Type} for query to execute.
	 * @param type the {@link R2dbcMessageHandler.Type} to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec queryType(R2dbcMessageHandler.Type type) {
		this.reactiveMessageHandler.setQueryType(type);
		return this;
	}

	/**
	 * Set a {@link Function} to evaluate a {@link R2dbcMessageHandler.Type} for query to execute against a request
	 * message.
	 * @param queryTypeFunction the function to use.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> R2dbcMessageHandlerSpec queryTypeFunction(
			Function<Message<P>, R2dbcMessageHandler.Type> queryTypeFunction) {

		return queryTypeExpression(new FunctionExpression<>(queryTypeFunction));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link R2dbcMessageHandler.Type} for query to execute.
	 * @param queryTypeExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec queryTypeExpression(String queryTypeExpression) {
		return queryTypeExpression(PARSER.parseExpression(queryTypeExpression));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link R2dbcMessageHandler.Type} for query to execute.
	 * @param queryTypeExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec queryTypeExpression(Expression queryTypeExpression) {
		this.reactiveMessageHandler.setQueryTypeExpression(queryTypeExpression);
		return this;
	}

	/**
	 * Specify a table in the target database to execute the query.
	 * @param tableName the name of the table to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec tableName(String tableName) {
		this.reactiveMessageHandler.setTableName(tableName);
		return this;
	}

	/**
	 * Set a {@link Function} to evaluate a table name at runtime against request message.
	 * @param tableNameFunction the function to use.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> R2dbcMessageHandlerSpec tableNameFunction(Function<Message<P>, String> tableNameFunction) {
		return tableNameExpression(new FunctionExpression<>(tableNameFunction));
	}

	/**
	 * Set a SpEL expression to evaluate a table name at runtime against request message.
	 * @param tableNameExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec tableNameExpression(String tableNameExpression) {
		return tableNameExpression(PARSER.parseExpression(tableNameExpression));
	}

	/**
	 * Set a SpEL expression to evaluate a table name at runtime against request message.
	 * @param tableNameExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec tableNameExpression(Expression tableNameExpression) {
		this.reactiveMessageHandler.setTableNameExpression(tableNameExpression);
		return this;
	}

	/**
	 * Set a {@link Function} to evaluate a {@link Map} for name-value pairs to bind as parameters
	 * into a query.
	 * @param valuesFunction the function to use.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> R2dbcMessageHandlerSpec values(Function<Message<P>, Map<String, ?>> valuesFunction) {
		return values(new FunctionExpression<>(valuesFunction));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Map} for name-value pairs to bind as parameters
	 * into a query.
	 * @param valuesExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec values(String valuesExpression) {
		return values(PARSER.parseExpression(valuesExpression));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Map} for name-value pairs to bind as parameters
	 * into a query.
	 * @param valuesExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec values(Expression valuesExpression) {
		this.reactiveMessageHandler.setValuesExpression(valuesExpression);
		return this;
	}

	/**
	 * Set a {@link Function} to evaluate a {@link Criteria} for query to execute.
	 * @param criteriaFunction the function to use.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> R2dbcMessageHandlerSpec criteria(Function<Message<P>, Criteria> criteriaFunction) {
		return criteria(new FunctionExpression<>(criteriaFunction));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Criteria} for query to execute.
	 * @param criteriaExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec criteria(String criteriaExpression) {
		return criteria(PARSER.parseExpression(criteriaExpression));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Criteria} for query to execute.
	 * @param criteriaExpression the expression to use.
	 * @return the spec
	 */
	public R2dbcMessageHandlerSpec criteria(Expression criteriaExpression) {
		this.reactiveMessageHandler.setCriteriaExpression(criteriaExpression);
		return this;
	}

}
