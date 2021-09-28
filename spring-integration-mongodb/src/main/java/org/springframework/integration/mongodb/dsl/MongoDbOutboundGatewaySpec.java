/*
 * Copyright 2016-2021 the original author or authors.
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

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mongodb.outbound.MessageCollectionCallback;
import org.springframework.integration.mongodb.outbound.MongoDbOutboundGateway;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} extension for the MongoDb Outbound endpoint {@link MongoDbOutboundGateway}.
 *
 * @author Xavier Padro
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MongoDbOutboundGatewaySpec
		extends MessageHandlerSpec<MongoDbOutboundGatewaySpec, MongoDbOutboundGateway> {

	protected MongoDbOutboundGatewaySpec(MongoDatabaseFactory mongoDbFactory, MongoConverter mongoConverter) {
		this.target = new MongoDbOutboundGateway(mongoDbFactory, mongoConverter);
		this.target.setRequiresReply(true);
	}

	protected MongoDbOutboundGatewaySpec(MongoOperations mongoTemplate) {
		this.target = new MongoDbOutboundGateway(mongoTemplate);
		this.target.setRequiresReply(true);
	}

	/**
	 * This parameter indicates that only one result object will be returned from the database
	 * by using a {@code findOne} query.
	 * If set to {@code false} (default), the complete result list is returned as the payload.
	 * @param expectSingleResult the {@code boolean} flag to indicate if a single result is returned or not.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

	/**
	 * A {@code String} representation of a MongoDb {@link Query} (e.g., query("{'name' : 'Bob'}")).
	 * Please refer to MongoDb documentation for more query samples
	 * see <a href="https://www.mongodb.org/display/DOCS/Querying">MongoDB Docs</a>
	 * This property is mutually exclusive with 'queryExpression' property.
	 * @param query the MongoDb {@link Query} string representation to use.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec query(String query) {
		this.target.setQueryExpression(new LiteralExpression(query));
		return this;
	}

	/**
	 * A SpEL expression which should resolve to a {@code String} query (please refer to the 'query' property),
	 * or to an instance of MongoDb {@link Query}
	 * (e.q., queryExpression("new BasicQuery('{''address.state'' : ''PA''}')")).
	 * @param queryExpression the SpEL expression query to use.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec queryExpression(String queryExpression) {
		this.target.setQueryExpressionString(queryExpression);
		return this;
	}

	/**
	 * A {@link Function} which should resolve to a {@link Query} instance.
	 * @param queryFunction the {@link Function} to use.
	 * @param <P> the type of the message payload.
	 * @return the spec
	 */
	public <P> MongoDbOutboundGatewaySpec queryFunction(Function<Message<P>, Query> queryFunction) {
		this.target.setQueryExpression(new FunctionExpression<>(queryFunction));
		return this;
	}

	/**
	 * The fully qualified name of the entity class to be passed
	 * to {@code find(..)} or {@code findOne(..)} method in {@link MongoOperations}.
	 * If this attribute is not provided the default value is {@link org.bson.Document}.
	 * @param entityClass the {@link Class} to use.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec entityClass(Class<?> entityClass) {
		this.target.setEntityClass(entityClass);
		return this;
	}

	/**
	 * Identify the name of the MongoDb collection to use.
	 * This attribute is mutually exclusive with {@link #collectionNameExpression} property.
	 * @param collectionName the {@link String} specifying the MongoDb collection.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec collectionName(String collectionName) {
		this.target.setCollectionNameExpression(new LiteralExpression(collectionName));
		return this;
	}

	/**
	 * A SpEL expression which should resolve to a {@link String} value
	 * identifying the name of the MongoDb collection to use.
	 * This property is mutually exclusive with {@link #collectionName} property.
	 * @param collectionNameExpression the {@link String} expression to use.
	 * @return the spec
	 */
	public MongoDbOutboundGatewaySpec collectionNameExpression(String collectionNameExpression) {
		this.target.setCollectionNameExpressionString(collectionNameExpression);
		return this;
	}

	/**
	 * A {@link Function} which should resolve to a {@link String}
	 * (e.q., {@code collectionNameFunction(Message::getPayload)}).
	 * @param collectionNameFunction the {@link Function} to use.
	 * @param <P> the type of the message payload.
	 * @return the spec
	 */
	public <P> MongoDbOutboundGatewaySpec collectionNameFunction(Function<Message<P>, String> collectionNameFunction) {
		this.target.setCollectionNameExpression(new FunctionExpression<>(collectionNameFunction));
		return this;
	}

	/**
	 * Reference to an instance of {@link MessageCollectionCallback}
	 * which specifies the database operation to execute in the request message context.
	 * This property is mutually exclusive with {@link #query} and {@link #queryExpression} properties.
	 * @param collectionCallback the {@link MessageCollectionCallback} instance
	 * @param <P> the type of the message payload.
	 * @return the spec
	 * @since 5.0.11
	 */
	public <P> MongoDbOutboundGatewaySpec collectionCallback(MessageCollectionCallback<P> collectionCallback) {
		this.target.setMessageCollectionCallback(collectionCallback);
		return this;
	}

}
