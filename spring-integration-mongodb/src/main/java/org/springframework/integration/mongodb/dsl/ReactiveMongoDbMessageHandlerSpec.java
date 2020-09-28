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

package org.springframework.integration.mongodb.dsl;

import java.util.function.Function;

import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.ReactiveMessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mongodb.outbound.ReactiveMongoDbStoringMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link ReactiveMessageHandlerSpec} extension for the Reactive MongoDb Outbound endpoint
 * {@link ReactiveMongoDbStoringMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbMessageHandlerSpec
		extends ReactiveMessageHandlerSpec<ReactiveMongoDbMessageHandlerSpec, ReactiveMongoDbStoringMessageHandler>
		implements ComponentsRegistration {

	protected ReactiveMongoDbMessageHandlerSpec(ReactiveMongoDatabaseFactory mongoDbFactory) {
		super(new ReactiveMongoDbStoringMessageHandler(mongoDbFactory));
	}

	protected ReactiveMongoDbMessageHandlerSpec(ReactiveMongoOperations reactiveMongoOperations) {
		super(new ReactiveMongoDbStoringMessageHandler(reactiveMongoOperations));
	}

	/**
	 * Configure a {@link MongoConverter}.
	 * @param mongoConverter the {@link MongoConverter} to use.
	 * @return the spec
	 */
	public ReactiveMongoDbMessageHandlerSpec mongoConverter(MongoConverter mongoConverter) {
		this.reactiveMessageHandler.setMongoConverter(mongoConverter);
		return this;
	}

	/**
	 * Configure a collection name to store data.
	 * @param collectionName the explicit collection name to use.
	 * @return the spec
	 */
	public ReactiveMongoDbMessageHandlerSpec collectionName(String collectionName) {
		return collectionNameExpression(new LiteralExpression(collectionName));
	}

	/**
	 * Configure a {@link Function} for evaluation a collection against request message.
	 * @param collectionNameFunction the {@link Function} to determine a collection name at runtime.
	 * @param <P> an expected payload type
	 * @return the spec
	 */
	public <P> ReactiveMongoDbMessageHandlerSpec collectionNameFunction(
			Function<Message<P>, String> collectionNameFunction) {

		return collectionNameExpression(new FunctionExpression<>(collectionNameFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a collection name against a request message.
	 * @param collectionNameExpression the SpEL expression to use.
	 * @return the spec
	 */
	public ReactiveMongoDbMessageHandlerSpec collectionNameExpression(Expression collectionNameExpression) {
		this.reactiveMessageHandler.setCollectionNameExpression(collectionNameExpression);
		return this;
	}

}
