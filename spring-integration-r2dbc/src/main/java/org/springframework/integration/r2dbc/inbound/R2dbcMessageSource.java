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

import org.reactivestreams.Publisher;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.relational.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;


/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of a {@link Query}. When {@code expectSingleResult} is false (default), the MongoDb
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
 *
 */
public class R2dbcMessageSource extends AbstractMessageSource<Publisher<?>>
		implements ApplicationContextAware {

	@Nullable
	private final DatabaseClient databaseClient;

	private final Expression queryExpression;

	private StandardEvaluationContext evaluationContext;

	private R2dbcEntityOperations r2dbcEntityOperations;

	private R2dbcConverter r2dbcConverter;

	private Class<?> entityClass;

	private boolean expectSingleResult = false;

	private ApplicationContext applicationContext;

	private volatile boolean initialized = false;

	/**
	 * Create an instance with the provided {@link DatabaseClient} and SpEL expression
	 * which should resolve to a R2dbc 'query' string.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param databaseClient The databaseClient .
	 * @param queryExpression The query expression.
	 */
	public R2dbcMessageSource(DatabaseClient databaseClient, Expression queryExpression) {

		Assert.notNull(databaseClient, "'databaseClient' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.databaseClient = databaseClient;
		this.queryExpression = queryExpression;
	}

	/**
	 * Create an instance with the provided {@link R2dbcEntityOperations} and SpEL expression
	 * which should resolve to a Mongo 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * It assumes that the {@link R2dbcEntityOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param r2dbcEntityOperations The reactive r2dbc template.
	 * @param queryExpression The query expression.
	 */
	public R2dbcMessageSource(R2dbcEntityOperations r2dbcEntityOperations, Expression queryExpression) {
		Assert.notNull(r2dbcEntityOperations, "'reactiveMongoTemplate' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.databaseClient = null;
		this.r2dbcEntityOperations = r2dbcEntityOperations;
		this.queryExpression = queryExpression;
	}

	/**
	 * Allow you to set the type of the entityClass that will be passed to the
	 * {@link R2dbcEntityOperations#select(Query, Class)} or {@link R2dbcEntityOperations#selectOne(Query, Class)}
	 * method.
	 * @param entityClass The entity class.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	/**
	 * Allow you to manage which find* method to invoke on {@link R2dbcEntityOperations}.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@link R2dbcEntityOperations#select(Query, Class)} method. If set to 'true',
	 * {@link #receive()} will use {@link R2dbcEntityOperations#selectOne(Query, Class)},
	 * and the payload of the returned {@link org.springframework.messaging.Message}
	 * will be the returned target Object of type
	 * identified by {@link #entityClass} instead of a List.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Allow you to provide a custom {@link R2dbcConverter} used to assist in deserialization
	 * data read from MongoDb. Only allowed if this instance was constructed with a
	 * {@link R2dbcRepositoryFactory}.
	 * @param r2dbcConverter The r2dbc converter.
	 */
	public void setMongoConverter(R2dbcConverter r2dbcConverter) {
		Assert.isNull(this.r2dbcEntityOperations,
				"'r2dbcConverter' can not be set when instance was constructed with R2dbcEntityOperations");
		this.r2dbcConverter = r2dbcConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public String getComponentType() {
		return "r2dbc:reactive-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			/*
			 * Register the R2dbc Query DSL package so they don't need a FQCN for QueryBuilder, for example.
			 */
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.r2dbc.repository.query");
		}
		if (this.databaseClient != null) {
			R2dbcEntityTemplate r2dbcEntityOperations =
					new R2dbcEntityTemplate(this.databaseClient);
			r2dbcEntityOperations.setBeanFactory(this.applicationContext.getParentBeanFactory());
			this.r2dbcEntityOperations = r2dbcEntityOperations;
		}
		this.initialized = true;
	}

	/**
	 * Execute a {@link Query} returning its results as the Message payload.
	 * The payload can be either {@link reactor.core.publisher.Flux} or
	 * {@link reactor.core.publisher.Mono} of objects of type identified by {@link #entityClass},
	 * or a single element of type identified by {@link #entityClass}
	 * based on the value of {@link #expectSingleResult} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link reactor.core.publisher.Flux}. The collection name used in the
	 */
	@Override
	public Object doReceive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Object value = this.queryExpression.getValue(this.evaluationContext);
		Assert.notNull(value, "'queryExpression' must not evaluate to null");
		Query query = null;
		if (value instanceof Query) {
			query = ((Query) value);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to String " +
					"or org.springframework.data.mongodb.core.query.Query, but not: " + query);
		}

		Object result;
		if (this.expectSingleResult) {
			result = this.r2dbcEntityOperations.selectOne(query, this.entityClass);
		}
		else {
			result = this.r2dbcEntityOperations.select(query, this.entityClass);
		}
		return getMessageBuilderFactory()
				.withPayload(result);
	}

}
