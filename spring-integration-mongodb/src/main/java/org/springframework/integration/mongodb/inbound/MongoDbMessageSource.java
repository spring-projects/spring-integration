/*
 * Copyright 2007-2021 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.expression.Expression;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * An instance of {@link org.springframework.integration.core.MessageSource} which returns
 * a {@link org.springframework.messaging.Message} with a payload which is the result of
 * execution of a {@link Query}. When expectSingleResult is false (default), the MongoDb
 * {@link Query} is executed using {@link MongoOperations#find(Query, Class)} method which
 * returns a {@link List}. The returned {@link List} will be used as the payload of the
 * {@link org.springframework.messaging.Message} returned by the {{@link #receive()}
 * method. An empty {@link List} is treated as null, thus resulting in no
 * {@link org.springframework.messaging.Message} returned by the {{@link #receive()}
 * method.
 * <p>
 * When expectSingleResult is true, the {@link MongoOperations#findOne(Query, Class)} is
 * used instead, and the message payload will be the single object returned from the
 * query.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Yaron Yamin
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 */
public class MongoDbMessageSource extends AbstractMongoDbMessageSource<Object> {

	@Nullable
	private final MongoDatabaseFactory mongoDbFactory;

	private MongoOperations mongoTemplate;

	/**
	 * Create an instance with the provided {@link MongoDatabaseFactory} and SpEL expression
	 * which should resolve to a MongoDb 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param mongoDbFactory The mongodb factory.
	 * @param queryExpression The query expression.
	 */
	public MongoDbMessageSource(MongoDatabaseFactory mongoDbFactory, Expression queryExpression) {
		super(queryExpression);
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");
		this.mongoDbFactory = mongoDbFactory;
	}

	/**
	 * Create an instance with the provided {@link MongoOperations} and SpEL expression
	 * which should resolve to a Mongo 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * It assumes that the {@link MongoOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param mongoTemplate The mongo template.
	 * @param queryExpression The query expression.
	 */
	public MongoDbMessageSource(MongoOperations mongoTemplate, Expression queryExpression) {
		super(queryExpression);
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");
		this.mongoDbFactory = null;
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public String getComponentType() {
		return "mongo:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.mongoDbFactory != null) {
			MongoTemplate template = new MongoTemplate(this.mongoDbFactory, getMongoConverter());
			ApplicationContext applicationContext = getApplicationContext();
			if (applicationContext != null) {
				template.setApplicationContext(applicationContext);
			}
			this.mongoTemplate = template;
		}
		setMongoConverter(this.mongoTemplate.getConverter());
		setInitialized(true);
	}

	/**
	 * Will execute a {@link Query} returning its results as the Message payload.
	 * The payload can be either {@link List} of elements of objects of type
	 * identified by {@link #getEntityClass()}, or a single element of type identified by {@link #getEntityClass()}
	 * based on the value of {@link #isExpectSingleResult()} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link List}. The collection name used in the
	 * query will be provided in the {@link MongoHeaders#COLLECTION_NAME} header.
	 */
	@Override
	protected Object doReceive() {
		Assert.isTrue(isInitialized(), "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		AbstractIntegrationMessageBuilder<Object> messageBuilder = null;

		Query query = evaluateQueryExpression();

		String collectionName = evaluateCollectionNameExpression();

		Object result = null;
		if (isExpectSingleResult()) {
			result = this.mongoTemplate.findOne(query, getEntityClass(), collectionName);
		}
		else {
			List<?> results = this.mongoTemplate.find(query, getEntityClass(), collectionName);
			if (!CollectionUtils.isEmpty(results)) {
				result = results;
			}
		}
		if (result != null) {
			updateIfAny(result, collectionName);
			messageBuilder =
					getMessageBuilderFactory()
							.withPayload(result)
							.setHeader(MongoHeaders.COLLECTION_NAME, collectionName);
		}

		Object holder = TransactionSynchronizationManager.getResource(this);
		if (holder != null) {
			Assert.isInstanceOf(IntegrationResourceHolder.class, holder);
			((IntegrationResourceHolder) holder).addAttribute("mongoTemplate", this.mongoTemplate);
		}

		return messageBuilder;
	}

	private void updateIfAny(Object result, String collectionName) {
		Update update = evaluateUpdateExpression();
		if (update != null) {
			if (result instanceof List) {
				this.mongoTemplate.updateMulti(getByIdInQuery((Collection<?>) result), update, collectionName);
			}
			else {
				Pair<String, Object> idPair = idForEntity(result);
				Query query = new Query(Criteria.where(idPair.getFirst()).is(idPair.getSecond()));
				this.mongoTemplate.updateFirst(query, update, collectionName);
			}
		}
	}


}
