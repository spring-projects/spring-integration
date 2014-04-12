/*
 * Copyright 2007-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.mongodb.outbound;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
/**
 * Implementation of {@link MessageHandler} which writes Message payload into a MongoDb collection
 * identified by evaluation of the {@link #collectionNameExpression}.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class MongoDbStoringMessageHandler extends AbstractMessageHandler {

	private volatile MongoOperations mongoTemplate;

	private volatile MongoDbFactory mongoDbFactory;

	private volatile MongoConverter mongoConverter;

	private volatile StandardEvaluationContext evaluationContext;

	private volatile Expression collectionNameExpression = new LiteralExpression("data");

	private volatile boolean initialized = false;

	/**
	 * Will construct this instance using provided {@link MongoDbFactory}
	 *
	 * @param mongoDbFactory The mongodb factory.
	 */
	public MongoDbStoringMessageHandler(MongoDbFactory mongoDbFactory){
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");

		this.mongoDbFactory = mongoDbFactory;
	}

	/**
	 * Will construct this instance using fully created and initialized instance of
	 * provided {@link MongoOperations}
	 *
	 * @param mongoTemplate The MongoOperations implementation.
	 */
	public MongoDbStoringMessageHandler(MongoOperations mongoTemplate){
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");

		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Allows you to provide custom {@link MongoConverter} used to assist in serialization
	 * of data written to MongoDb. Only allowed if this instance was constructed with a
	 * {@link MongoDbFactory}.
	 *
	 * @param mongoConverter The mongo converter.
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.isNull(this.mongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with MongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	/**
	 * Sets the SpEL {@link Expression} that should resolve to a collection name
	 * used by {@link MongoOperations} to store data
	 *
	 * @param collectionNameExpression The collection name expression.
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "'collectionNameExpression' must not be null");
		this.collectionNameExpression = collectionNameExpression;
	}

	@Override
	public String getComponentType() {
		return "mongo:outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		this.evaluationContext =
					ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		if (this.mongoTemplate == null){
			this.mongoTemplate = new MongoTemplate(this.mongoDbFactory, this.mongoConverter);
		}
		this.initialized = true;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		String collectionName = this.collectionNameExpression.getValue(this.evaluationContext, message, String.class);
		Assert.notNull(collectionName, "'collectionNameExpression' must not evaluate to null");

		Object payload = message.getPayload();

		this.mongoTemplate.save(payload, collectionName);
	}
}
