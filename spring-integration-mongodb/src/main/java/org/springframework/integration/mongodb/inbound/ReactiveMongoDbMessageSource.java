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

package org.springframework.integration.mongodb.inbound;

import org.reactivestreams.Publisher;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.mongodb.DBObject;

/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of a {@link Query}. When {@code expectSingleResult} is false (default), the MongoDb
 * {@link Query} is executed using {@link ReactiveMongoOperations#find(Query, Class)} method which
 * returns a {@link reactor.core.publisher.Flux}.
 * The returned {@link reactor.core.publisher.Flux} will be used as the payload of the
 * {@link org.springframework.messaging.Message} returned by the {@link #receive()}
 * method.
 * <p>
 * When {@code expectSingleResult} is true, the {@link ReactiveMongoOperations#findOne(Query, Class)} is
 * used instead, and the message payload will be a {@link reactor.core.publisher.Mono}
 * for the single object returned from the query.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbMessageSource extends AbstractMessageSource<Publisher<?>>
		implements ApplicationContextAware {

	@Nullable
	private final ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory;

	private final Expression queryExpression;

	private Expression collectionNameExpression = new LiteralExpression("data");

	private StandardEvaluationContext evaluationContext;

	private ReactiveMongoOperations reactiveMongoTemplate;

	private MongoConverter mongoConverter;

	private Class<?> entityClass = DBObject.class;

	private boolean expectSingleResult = false;

	private ApplicationContext applicationContext;

	private volatile boolean initialized = false;

	/**
	 * Create an instance with the provided {@link ReactiveMongoDatabaseFactory} and SpEL expression
	 * which should resolve to a MongoDb 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param reactiveMongoDatabaseFactory The reactiveMongoDatabaseFactory factory.
	 * @param queryExpression The query expression.
	 */
	public ReactiveMongoDbMessageSource(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			Expression queryExpression) {

		Assert.notNull(reactiveMongoDatabaseFactory, "'reactiveMongoDatabaseFactory' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.reactiveMongoDatabaseFactory = reactiveMongoDatabaseFactory;
		this.queryExpression = queryExpression;
	}

	/**
	 * Create an instance with the provided {@link ReactiveMongoOperations} and SpEL expression
	 * which should resolve to a Mongo 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * It assumes that the {@link ReactiveMongoOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param reactiveMongoTemplate The reactive Mongo template.
	 * @param queryExpression The query expression.
	 */
	public ReactiveMongoDbMessageSource(ReactiveMongoOperations reactiveMongoTemplate, Expression queryExpression) {
		Assert.notNull(reactiveMongoTemplate, "'reactiveMongoTemplate' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.reactiveMongoDatabaseFactory = null;
		this.reactiveMongoTemplate = reactiveMongoTemplate;
		this.queryExpression = queryExpression;
	}

	/**
	 * Allow you to set the type of the entityClass that will be passed to the
	 * {@link ReactiveMongoTemplate#find(Query, Class)} or {@link ReactiveMongoTemplate#findOne(Query, Class)}
	 * method.
	 * Default is {@link DBObject}.
	 * @param entityClass The entity class.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	/**
	 * Allow you to manage which find* method to invoke on {@link ReactiveMongoTemplate}.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@link ReactiveMongoTemplate#find(Query, Class)} method. If set to 'true',
	 * {@link #receive()} will use {@link ReactiveMongoTemplate#findOne(Query, Class)},
	 * and the payload of the returned {@link org.springframework.messaging.Message}
	 * will be the returned target Object of type
	 * identified by {@link #entityClass} instead of a List.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Set the SpEL {@link Expression} that should resolve to a collection name
	 * used by the {@link Query}. The resulting collection name will be included
	 * in the {@link MongoHeaders#COLLECTION_NAME} header.
	 * @param collectionNameExpression The collection name expression.
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "'collectionNameExpression' must not be null");
		this.collectionNameExpression = collectionNameExpression;
	}

	/**
	 * Allow you to provide a custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb. Only allowed if this instance was constructed with a
	 * {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoConverter The mongo converter.
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.isNull(this.reactiveMongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with ReactiveMongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public String getComponentType() {
		return "mongo:reactive-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			//Register MongoDB query API package so FQCN can be avoided in query-expression.
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.mongodb.core.query");
		}
		if (this.reactiveMongoDatabaseFactory != null) {
			ReactiveMongoTemplate template =
					new ReactiveMongoTemplate(this.reactiveMongoDatabaseFactory, this.mongoConverter);
			if (this.applicationContext != null) {
				template.setApplicationContext(this.applicationContext);
			}
			this.reactiveMongoTemplate = template;
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
	 * query will be provided in the {@link MongoHeaders#COLLECTION_NAME} header.
	 */
	@Override
	public Object doReceive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Object value = this.queryExpression.getValue(this.evaluationContext);
		Assert.notNull(value, "'queryExpression' must not evaluate to null");
		Query query = null;
		if (value instanceof String) {
			query = new BasicQuery((String) value);
		}
		else if (value instanceof Query) {
			query = ((Query) value);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to String " +
					"or org.springframework.data.mongodb.core.query.Query, but not: " + query);
		}

		String collectionName = this.collectionNameExpression.getValue(this.evaluationContext, String.class);
		Assert.notNull(collectionName, "'collectionNameExpression' must not evaluate to null");

		Object result;
		if (this.expectSingleResult) {
			result = this.reactiveMongoTemplate.findOne(query, this.entityClass, collectionName);
		}
		else {
			result = this.reactiveMongoTemplate.find(query, this.entityClass, collectionName);
		}
		return getMessageBuilderFactory()
				.withPayload(result)
				.setHeader(MongoHeaders.COLLECTION_NAME, collectionName);
	}

}
