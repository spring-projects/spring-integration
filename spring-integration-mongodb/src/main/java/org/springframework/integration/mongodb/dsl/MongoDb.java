/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.mongodb.inbound.MongoDbChangeStreamMessageProducer;

/**
 * Factory class for building MongoDb components
 *
 * @author Xavier Padro
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class MongoDb {

	/**
	 * Create a {@link MongoDbOutboundGatewaySpec} builder instance
	 * based on the provided {@link MongoDatabaseFactory} and {@link MongoConverter}.
	 * @param mongoDbFactory the {@link MongoDatabaseFactory} to use.
	 * @param mongoConverter the {@link MongoConverter} to use.
	 * @return the {@link MongoDbOutboundGatewaySpec} instance
	 */
	public static MongoDbOutboundGatewaySpec outboundGateway(
			MongoDatabaseFactory mongoDbFactory, MongoConverter mongoConverter) {

		return new MongoDbOutboundGatewaySpec(mongoDbFactory, mongoConverter);
	}

	/**
	 * Create a {@link MongoDbOutboundGatewaySpec} builder instance
	 * based on the provided {@link MongoOperations}.
	 * @param mongoTemplate the {@link MongoOperations} to use.
	 * @return the {@link MongoDbOutboundGatewaySpec} instance
	 */
	public static MongoDbOutboundGatewaySpec outboundGateway(MongoOperations mongoTemplate) {
		return new MongoDbOutboundGatewaySpec(mongoTemplate);
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageHandlerSpec} builder instance
	 * based on the provided {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoDbFactory the {@link ReactiveMongoDatabaseFactory} to use.
	 * @return the {@link MongoDbOutboundGatewaySpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageHandlerSpec reactiveOutboundChannelAdapter(
			ReactiveMongoDatabaseFactory mongoDbFactory) {

		return new ReactiveMongoDbMessageHandlerSpec(mongoDbFactory);
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageHandlerSpec} builder instance
	 * based on the provided {@link ReactiveMongoOperations}.
	 * @param mongoTemplate the {@link ReactiveMongoOperations} to use.
	 * @return the {@link ReactiveMongoDbMessageHandlerSpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageHandlerSpec reactiveOutboundChannelAdapter(
			ReactiveMongoOperations mongoTemplate) {

		return new ReactiveMongoDbMessageHandlerSpec(mongoTemplate);
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageSourceSpec} builder instance
	 * based on the provided {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoDbFactory the {@link ReactiveMongoDatabaseFactory} to use.
	 * @param query the MongoDb query
	 * @return the {@link ReactiveMongoDbMessageSourceSpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageSourceSpec reactiveInboundChannelAdapter(
			ReactiveMongoDatabaseFactory mongoDbFactory, String query) {

		return new ReactiveMongoDbMessageSourceSpec(mongoDbFactory, new LiteralExpression(query));
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageSourceSpec} builder instance
	 * based on the provided {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoDbFactory the {@link ReactiveMongoDatabaseFactory} to use.
	 * @param query the MongoDb query DSL object
	 * @return the {@link ReactiveMongoDbMessageSourceSpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageSourceSpec reactiveInboundChannelAdapter(
			ReactiveMongoDatabaseFactory mongoDbFactory, Query query) {

		return new ReactiveMongoDbMessageSourceSpec(mongoDbFactory, new ValueExpression<>(query));
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageSourceSpec} builder instance
	 * based on the provided {@link ReactiveMongoOperations}.
	 * @param mongoTemplate the {@link ReactiveMongoOperations} to use.
	 * @param query the MongoDb query
	 * @return the {@link ReactiveMongoDbMessageSourceSpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageSourceSpec reactiveInboundChannelAdapter(ReactiveMongoOperations mongoTemplate,
			String query) {

		return new ReactiveMongoDbMessageSourceSpec(mongoTemplate, new LiteralExpression(query));
	}

	/**
	 * Create a {@link ReactiveMongoDbMessageSourceSpec} builder instance
	 * based on the provided {@link ReactiveMongoOperations}.
	 * @param mongoTemplate the {@link ReactiveMongoOperations} to use.
	 * @param query the MongoDb query DSL object
	 * @return the {@link ReactiveMongoDbMessageSourceSpec} instance
	 * @since 5.3
	 */
	public static ReactiveMongoDbMessageSourceSpec reactiveInboundChannelAdapter(ReactiveMongoOperations mongoTemplate,
			Query query) {

		return new ReactiveMongoDbMessageSourceSpec(mongoTemplate, new ValueExpression<>(query));
	}

	/**
	 * Create a {@link MongoDbChangeStreamMessageProducerSpec} builder instance
	 * based on the provided {@link ReactiveMongoOperations}.
	 * @param mongoOperations the {@link ReactiveMongoOperations} to use.
	 * @return the {@link MongoDbChangeStreamMessageProducerSpec} instance
	 * @since 5.3
	 */
	public static MongoDbChangeStreamMessageProducerSpec changeStreamInboundChannelAdapter(
			ReactiveMongoOperations mongoOperations) {

		return new MongoDbChangeStreamMessageProducerSpec(new MongoDbChangeStreamMessageProducer(mongoOperations));
	}

	private MongoDb() {
	}

}
