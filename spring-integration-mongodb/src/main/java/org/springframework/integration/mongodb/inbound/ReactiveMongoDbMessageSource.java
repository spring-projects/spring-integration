/*
 * Copyright 2020-2021 the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.expression.Expression;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
public class ReactiveMongoDbMessageSource extends AbstractMongoDbMessageSource<Publisher<?>> {

	@Nullable
	private final ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory;

	private ReactiveMongoOperations reactiveMongoTemplate;

	/**
	 * Create an instance with the provided {@link ReactiveMongoDatabaseFactory} and SpEL expression
	 * which should resolve to a MongoDb 'query' string (see https://www.mongodb.org/display/DOCS/Querying).
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 * @param reactiveMongoDatabaseFactory The reactiveMongoDatabaseFactory factory.
	 * @param queryExpression The query expression.
	 */
	public ReactiveMongoDbMessageSource(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			Expression queryExpression) {

		super(queryExpression);
		Assert.notNull(reactiveMongoDatabaseFactory, "'reactiveMongoDatabaseFactory' must not be null");
		this.reactiveMongoDatabaseFactory = reactiveMongoDatabaseFactory;
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
		super(queryExpression);
		Assert.notNull(reactiveMongoTemplate, "'reactiveMongoTemplate' must not be null");
		this.reactiveMongoDatabaseFactory = null;
		this.reactiveMongoTemplate = reactiveMongoTemplate;
	}

	@Override
	public String getComponentType() {
		return "mongo:reactive-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.reactiveMongoDatabaseFactory != null) {
			ReactiveMongoTemplate template =
					new ReactiveMongoTemplate(this.reactiveMongoDatabaseFactory, getMongoConverter());
			ApplicationContext applicationContext = getApplicationContext();
			if (applicationContext != null) {
				template.setApplicationContext(applicationContext);
			}
			this.reactiveMongoTemplate = template;
		}
		setMongoConverter(this.reactiveMongoTemplate.getConverter());
		setInitialized(true);
	}

	/**
	 * Execute a {@link Query} returning its results as the Message payload.
	 * The payload can be either {@link reactor.core.publisher.Flux} or
	 * {@link reactor.core.publisher.Mono} of objects of type identified by {@link #getEntityClass()},
	 * or a single element of type identified by {@link #getEntityClass()}
	 * based on the value of {@link #isExpectSingleResult()} attribute which defaults to 'false' resulting
	 * {@link org.springframework.messaging.Message} with payload of type
	 * {@link reactor.core.publisher.Flux}. The collection name used in the
	 * query will be provided in the {@link MongoHeaders#COLLECTION_NAME} header.
	 */
	@Override
	public Object doReceive() {
		Assert.isTrue(isInitialized(), "This class is not yet initialized. Invoke its afterPropertiesSet() method");

		Query query = evaluateQueryExpression();

		String collectionName = evaluateCollectionNameExpression();

		Publisher<?> result;
		if (isExpectSingleResult()) {
			result = this.reactiveMongoTemplate.findOne(query, getEntityClass(), collectionName);
		}
		else {
			result = this.reactiveMongoTemplate.find(query, getEntityClass(), collectionName);
		}

		result = updateIfAny(result, collectionName);

		return getMessageBuilderFactory()
				.withPayload(result)
				.setHeader(MongoHeaders.COLLECTION_NAME, collectionName);
	}

	private Publisher<?> updateIfAny(Publisher<?> result, String collectionName) {
		Update update = evaluateUpdateExpression();
		if (update != null) {
			if (result instanceof Mono) {
				return updateSingle((Mono<?>) result, update, collectionName);
			}
			else {
				return updateMulti((Flux<?>) result, update, collectionName);
			}
		}
		else {
			return result;
		}
	}

	private Publisher<?> updateSingle(Mono<?> result, Update update, String collectionName) {
		return result.flatMap((entity) -> {
			Pair<String, Object> idPair = idForEntity(entity);
			Query query = new Query(Criteria.where(idPair.getFirst()).is(idPair.getSecond()));
			return this.reactiveMongoTemplate.updateFirst(query, update, collectionName)
					.thenReturn(entity);
		});
	}

	private Publisher<?> updateMulti(Flux<?> result, Update update, String collectionName) {
		return result.collectList()
				.flatMapMany((entities) ->
						this.reactiveMongoTemplate.updateMulti(getByIdInQuery(entities), update, collectionName)
								.thenMany(Flux.fromIterable(entities))
				);
	}

}
