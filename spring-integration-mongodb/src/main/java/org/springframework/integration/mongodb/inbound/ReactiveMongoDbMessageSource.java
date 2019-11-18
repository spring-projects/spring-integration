/*
 * Copyright 2019 the original author or authors.
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

import java.util.List;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
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
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.mongodb.DBObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of a {@link Query}. When expectSingleResult is false (default), the MongoDb
 * {@link Query} is executed using {@link ReactiveMongoOperations#find(Query, Class)} method which
 * returns a {@link List}. The returned {@link List} will be used as the payoad of the
 * {@link org.springframework.messaging.Message} returned by the {{@link #receive()}
 * method. An empty {@link List} is treated as null, thus resulting in no
 * {@link org.springframework.messaging.Message} returned by the {{@link #receive()}
 * method.
 * <p>
 * When expectSingleResult is true, the {@link ReactiveMongoOperations#findOne(Query, Class)} is
 * used instead, and the message payload will be the single object returned from the
 * query.
 *
 * @author David Turanski
 *
 * @since 5.2.2
 */
public class ReactiveMongoDbMessageSource extends AbstractMessageSource<Object> {

	private final Expression queryExpression;

	private volatile Expression collectionNameExpression = new LiteralExpression("data");

	private volatile StandardEvaluationContext evaluationContext;

	private volatile ReactiveMongoOperations mongoTemplate;

	private volatile MongoConverter mongoConverter;

	private volatile ReactiveMongoDatabaseFactory mongoDbFactory;

	private volatile boolean initialized = false;

	private volatile Class<?> entityClass = DBObject.class;

	private volatile boolean expectSingleResult = false;

	/**
	 * Creates an instance with the provided {@link ReactiveMongoDatabaseFactory} and SpEL expression
	 * which should resolve to a MongoDb 'query' string
	 * (see https://www.mongodb.org/display/DOCS/Querying).
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param mongoDbFactory The mongodb factory.
	 * @param queryExpression The query expression.
	 */
	public ReactiveMongoDbMessageSource(ReactiveMongoDatabaseFactory mongoDbFactory, Expression queryExpression) {
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");

		this.mongoDbFactory = mongoDbFactory;
		this.queryExpression = queryExpression;
	}

	/**
	 * Creates an instance with the provided {@link ReactiveMongoOperations} and SpEL expression
	 * which should resolve to a Mongo 'query' string
	 * (see https://www.mongodb.org/display/DOCS/Querying).
	 * It assumes that the {@link ReactiveMongoOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param mongoTemplate The mongo template.
	 * @param queryExpression The query expression.
	 */
	public ReactiveMongoDbMessageSource(ReactiveMongoOperations mongoTemplate, Expression queryExpression) {
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");

		this.mongoTemplate = mongoTemplate;
		this.queryExpression = queryExpression;
	}

	/**
	 * Allows you to set the type of the entityClass that will be passed to the
	 * {@link MongoTemplate#find(Query, Class)} or {@link MongoTemplate#findOne(Query, Class)}
	 * method.
	 * Default is {@link DBObject}.
	 * @param entityClass The entity class.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	/**
	 * Allows you to manage which find* method to invoke on {@link MongoTemplate}.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@link MongoTemplate#find(Query, Class)} method. If set to 'true',
	 * {@link #receive()} will use {@link MongoTemplate#findOne(Query, Class)},
	 * and the payload of the returned {@link org.springframework.messaging.Message}
	 * will be the returned target Object of type
	 * identified by {{@link #entityClass} instead of a List.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Sets the SpEL {@link Expression} that should resolve to a collection name
	 * used by the {@link Query}. The resulting collection name will be included
	 * in the {@link MongoHeaders#COLLECTION_NAME} header.
	 * @param collectionNameExpression The collection name expression.
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "'collectionNameExpression' must not be null");
		this.collectionNameExpression = collectionNameExpression;
	}

	/**
	 * Allows you to provide a custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb. Only allowed if this instance was constructed with a
	 * {@link MongoDbFactory}.
	 * @param mongoConverter The mongo converter.
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.isNull(this.mongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with MongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	@Override
	public String getComponentType() {
		return "mongo:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.evaluationContext =
				ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			//Register MongoDB query API package so FQCN can be avoided in query-expression.
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.mongodb.core.query");
		}
		if (this.mongoTemplate == null) {
			this.mongoTemplate = new ReactiveMongoTemplate(this.mongoDbFactory, this.mongoConverter);
		}
		this.initialized = true;
	}

	/**
	 * Will execute a {@link Query} returning its results as the Message payload.
	 * The payload can be either {@link Flux} of elements of objects of type
	 * identified by {{@link #entityClass}, or a {@link Mono} of type identified by {{@link #entityClass}
	 * based on the value of {{@link #expectSingleResult} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link Flux}. The collection name used in the
	 * query will be provided in the {@link MongoHeaders#COLLECTION_NAME} header.
	 */
	@Override
	protected Object doReceive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		AbstractIntegrationMessageBuilder<Object> messageBuilder = null;
		Object value = this.queryExpression.getValue(this.evaluationContext);
		Assert.notNull(value, "'queryExpression' must not evaluate to null");
		Query query;
		if (value instanceof String) {
			query = new BasicQuery((String) value);
		}
		else if (value instanceof Query) {
			query = ((Query) value);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to String " +
					"or org.springframework.data.mongodb.core.query.Query");
		}

		Assert.notNull(query, "'queryExpression' must not evaluate to null");
		String collectionName = this.collectionNameExpression.getValue(this.evaluationContext, String.class);
		Assert.notNull(collectionName, "'collectionNameExpression' must not evaluate to null");

		Object result;
		if (this.expectSingleResult) {
			result = this.mongoTemplate.
					findOne(query, this.entityClass, collectionName);
		}
		else {
			Flux<?> results = this.mongoTemplate.
					find(query, this.entityClass, collectionName);
			result = results;

		}

		messageBuilder = this.getMessageBuilderFactory().withPayload(result)
				.setHeader(MongoHeaders.COLLECTION_NAME, collectionName);


		Object holder = TransactionSynchronizationManager.getResource(this);
		if (holder != null) {
			Assert.isInstanceOf(IntegrationResourceHolder.class, holder);
			((IntegrationResourceHolder) holder).addAttribute("mongoTemplate", this.mongoTemplate);
		}

		return messageBuilder;
	}

}
