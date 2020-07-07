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

import org.reactivestreams.Publisher;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.util.Assert;

/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of a {@link Query}. When {@code expectSingleResult} is false (default), the R2dbc
 * {@link Query} is executed using {@link R2dbcEntityOperations#select(Query, Class)} method which
 * returns a {@link reactor.core.publisher.Flux}.
 * The returned {@link reactor.core.publisher.Flux} will be used as the payload of the
 * {@link org.springframework.messaging.Message} returned by the {@link #receive()}
 * method.
 * <p>
 * When {@code expectSingleResult} is true, the {@link R2dbcEntityOperations#selectOne(Query, Class)} is
 * used instead, and the message payload will be a {@link reactor.core.publisher.Mono}
 * for the single object returned from the query.
 *
 * @author Rohan Mukesh
 * @since 5.4
 */
public class R2dbcMessageSource extends AbstractMessageSource<Publisher<?>> {

	private final Expression queryExpression;

	private final R2dbcEntityOperations r2dbcEntityOperations;

	private Class<?> payloadType = Map.class;

	private boolean expectSingleResult = false;

	private StandardEvaluationContext evaluationContext;

	private volatile boolean initialized = false;

	/**
	 * Create an instance with the provided {@link R2dbcEntityOperations} and SpEL expression
	 * which should resolve to a Relational 'query' string.
	 * It assumes that the {@link R2dbcEntityOperations} is fully initialized and ready to be used.
	 * The 'query' will be evaluated on every call to the {@link #receive()} method.
	 * @param r2dbcEntityOperations The reactive r2dbc template.
	 * @param query                 The query String.
	 */
	public R2dbcMessageSource(R2dbcEntityOperations r2dbcEntityOperations, String query) {
		this(r2dbcEntityOperations, new LiteralExpression(query));
	}

	/**
	 * Create an instance with the provided {@link R2dbcEntityOperations} and SpEL expression
	 * which should resolve to a Relational 'query' string.
	 * It assumes that the {@link R2dbcEntityOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param r2dbcEntityOperations The reactive r2dbc template.
	 * @param queryExpression       The query expression.
	 */
	public R2dbcMessageSource(R2dbcEntityOperations r2dbcEntityOperations, Expression queryExpression) {
		Assert.notNull(r2dbcEntityOperations, "'r2dbcEntityOperations' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.r2dbcEntityOperations = r2dbcEntityOperations;
		this.queryExpression = queryExpression;
	}

	/**
	 * Provide a way to set the type of the entityClass that will be passed to the
	 * {@link org.springframework.data.r2dbc.core.DatabaseClient#execute(String)}
	 * method.
	 * @param payloadType The t class.
	 */
	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType = payloadType;
	}

	/**
	 * Provide a way to manage which find* method to invoke on {@link R2dbcEntityOperations}.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@link org.springframework.data.r2dbc.core.DatabaseClient#execute(String)} method and will fetch all. If set
	 * to 'true'{@link #receive()} will use {@link org.springframework.data.r2dbc.core.DatabaseClient#execute(String)}
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
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			/*
			 * Register the R2dbc Query DSL package so they don't need a FQCN for QueryBuilder, for example.
			 */
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.relational.core.query");
		}
		this.initialized = true;
	}

	/**
	 * Execute a {@link Query} returning its results as the Message payload.
	 * The payload can be either {@link reactor.core.publisher.Flux} or
	 * {@link reactor.core.publisher.Mono} of objects of type identified by {@link #payloadType},
	 * or a single element of type identified by {@link #payloadType}
	 * based on the value of {@link #expectSingleResult} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link reactor.core.publisher.Flux}. The collection name used in the
	 */
	@Override
	protected Object doReceive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Object value = this.queryExpression.getValue(this.evaluationContext);
		Object result;
		if (value instanceof String) {
			result = executeQuery((String) value);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to String " +
					"or org.springframework.data.relational.core.query.Query, but not: " + value);
		}
		return getMessageBuilderFactory().withPayload(result);
	}

	private Object executeQuery(String queryString) {
		Object result;
		if (this.expectSingleResult) {
			result = this.r2dbcEntityOperations.getDatabaseClient()
					.execute(queryString)
					.as(this.payloadType)
					.fetch()
					.one();
		}
		else {
			result = this.r2dbcEntityOperations.getDatabaseClient()
					.execute(queryString)
					.as(this.payloadType)
					.fetch()
					.all();
		}
		return result;
	}
}
