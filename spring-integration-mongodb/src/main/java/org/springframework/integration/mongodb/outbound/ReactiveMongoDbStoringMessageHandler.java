/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.integration.mongodb.outbound;

import reactor.core.publisher.Mono;

import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.messaging.ReactiveMessageHandler} which writes
 * Message payload into a MongoDb collection, using reactive MongoDb support, The
 * collection is identified by evaluation of the {@link #collectionNameExpression}.
 *
 * @author David Turanski
 * @author Artme Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbStoringMessageHandler extends AbstractReactiveMessageHandler {

	private ReactiveMongoOperations mongoTemplate;

	private ReactiveMongoDatabaseFactory mongoDbFactory;

	private MongoConverter mongoConverter;

	private StandardEvaluationContext evaluationContext;

	private Expression collectionNameExpression = new LiteralExpression("data");

	private volatile boolean initialized = false;

	/**
	 * Construct this instance using a provided {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoDbFactory The reactive mongoDatabase factory.
	 */
	public ReactiveMongoDbStoringMessageHandler(ReactiveMongoDatabaseFactory mongoDbFactory) {
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");
		this.mongoDbFactory = mongoDbFactory;
	}

	/**
	 * Construct this instance using a fully created and initialized instance of provided
	 * {@link ReactiveMongoOperations}.
	 * @param mongoTemplate The ReactiveMongoOperations implementation.
	 */
	public ReactiveMongoDbStoringMessageHandler(ReactiveMongoOperations mongoTemplate) {
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Provide a custom {@link MongoConverter} used to assist in serialization of
	 * data written to MongoDb. Only allowed if this instance was constructed with a
	 * {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoConverter The mongo converter.
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.isNull(this.mongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with MongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	/**
	 * Set a SpEL {@link Expression} that should resolve to a collection name used by
	 * {@link ReactiveMongoOperations} to store data
	 * @param collectionNameExpression The collection name expression.
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "'collectionNameExpression' must not be null");
		this.collectionNameExpression = collectionNameExpression;
	}

	@Override
	public String getComponentType() {
		return "mongo:reactive-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.mongoTemplate == null) {
			ReactiveMongoTemplate template = new ReactiveMongoTemplate(this.mongoDbFactory, this.mongoConverter);
			template.setApplicationContext(getApplicationContext());
			this.mongoTemplate = template;
		}
		this.initialized = true;
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");

		return evaluateCollectionNameExpression(message)
				.flatMap(collection -> this.mongoTemplate.save(message.getPayload(), collection))
				.then();
	}

	private Mono<String> evaluateCollectionNameExpression(Message<?> message) {
		return Mono.fromSupplier(() -> {
			String collectionName =
					this.collectionNameExpression.getValue(this.evaluationContext, message, String.class);
			Assert.notNull(collectionName, "'collectionNameExpression' must not evaluate to null");
			return collectionName;
		});
	}

}
