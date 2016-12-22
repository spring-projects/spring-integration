/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mongodb.dsl;

import java.util.function.Function;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mongodb.outbound.MongoDbOutboundGateway;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} extension for the MongoDb Outbound endpoint {@link MongoDbOutboundGateway}
 *
 * @author Xavier Padró
 * @since 5.0
 */
public class MongoDbOutboundGatewaySpec
		extends MessageHandlerSpec<MongoDbOutboundGatewaySpec, MongoDbOutboundGateway> {

	MongoDbOutboundGatewaySpec(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
		this.target = new MongoDbOutboundGateway(mongoDbFactory, mongoConverter);
		this.target.setRequiresReply(true);
	}

	MongoDbOutboundGatewaySpec(MongoOperations mongoTemplate) {
		this.target = new MongoDbOutboundGateway(mongoTemplate);
		this.target.setRequiresReply(true);
	}

	public MongoDbOutboundGatewaySpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

	public MongoDbOutboundGatewaySpec query(String query) {
		this.target.setQueryExpression(new LiteralExpression(query));
		return this;
	}

	public MongoDbOutboundGatewaySpec queryExpression(String queryExpression) {
		this.target.setQueryExpression(PARSER.parseExpression(queryExpression));
		return this;
	}

	public <P> MongoDbOutboundGatewaySpec queryFunction(Function<Message<P>, Query> queryFunction) {
		this.target.setQueryExpression(new FunctionExpression<>(queryFunction));
		return this;
	}

	public MongoDbOutboundGatewaySpec entityClass(Class<?> entityClass) {
		this.target.setEntityClass(entityClass);
		return this;
	}

	public MongoDbOutboundGatewaySpec collectionName(String collectionName) {
		this.target.setCollectionNameExpression(new LiteralExpression(collectionName));
		return this;
	}

	public MongoDbOutboundGatewaySpec collectionNameExpression(String collectionNameExpression) {
		this.target.setCollectionNameExpressionString(collectionNameExpression);
		return this;
	}

	public <P> MongoDbOutboundGatewaySpec collectionNameFunction(Function<Message<P>, String> collectionNameFunction) {
		this.target.setCollectionNameExpression(new FunctionExpression<>(collectionNameFunction));
		return this;
	}
}
