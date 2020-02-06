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

import java.util.function.Supplier;

import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.mongodb.inbound.ReactiveMongoDbMessageSource;

/**
 * A {@link MessageSourceSpec} implementation for a {@link ReactiveMongoDbMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbMessageSourceSpec
		extends MessageSourceSpec<ReactiveMongoDbMessageSourceSpec, ReactiveMongoDbMessageSource> {

	protected ReactiveMongoDbMessageSourceSpec(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			Expression queryExpression) {

		this.target = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory, queryExpression);
	}

	protected ReactiveMongoDbMessageSourceSpec(ReactiveMongoOperations reactiveMongoTemplate,
			Expression queryExpression) {

		this.target = new ReactiveMongoDbMessageSource(reactiveMongoTemplate, queryExpression);
	}

	/**
	 * Allow you to set the type of the entityClass that will be passed to the
	 * {@link ReactiveMongoOperations#find} or {@link ReactiveMongoOperations#findOne}
	 * method.
	 * Default is {@link com.mongodb.DBObject}.
	 * @param entityClass The entity class.
	 * @return the spec
	 * @see ReactiveMongoDbMessageSource#setEntityClass(Class)
	 */
	public ReactiveMongoDbMessageSourceSpec entityClass(Class<?> entityClass) {
		this.target.setEntityClass(entityClass);
		return this;
	}

	/**
	 * Allow you to manage which find* method to invoke on {@link ReactiveMongoOperations}.
	 * @param expectSingleResult true if a single result is expected.
	 * @return the spec
	 * @see ReactiveMongoDbMessageSource#setExpectSingleResult(boolean)
	 */
	public ReactiveMongoDbMessageSourceSpec expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return this;
	}

	/**
	 * Configure a collection name to query against.
	 * @param collectionName the name of the MongoDb collection
	 * @return the spec
	 */
	public ReactiveMongoDbMessageSourceSpec collectionName(String collectionName) {
		return collectionNameExpression(new LiteralExpression(collectionName));
	}

	/**
	 * Configure a SpEL expression to evaluation a collection name on each {@code receive()} call.
	 * @param collectionNameExpression the SpEL expression for name of the MongoDb collection
	 * @return the spec
	 */
	public ReactiveMongoDbMessageSourceSpec collectionNameExpression(String collectionNameExpression) {
		return collectionNameExpression(PARSER.parseExpression(collectionNameExpression));
	}

	/**
	 * Configure a {@link Supplier} to obtain a collection name on each {@code receive()} call.
	 * @param collectionNameSupplier the {@link Supplier} for name of the MongoDb collection
	 * @return the spec
	 */
	public ReactiveMongoDbMessageSourceSpec collectionNameSupplier(Supplier<String> collectionNameSupplier) {
		return collectionNameExpression(new SupplierExpression<>(collectionNameSupplier));
	}

	/**
	 * Configure a SpEL expression to evaluation a collection name on each {@code receive()} call.
	 * @param collectionNameExpression the SpEL expression for name of the MongoDb collection
	 * @return the spec
	 * @see ReactiveMongoDbMessageSource#setCollectionNameExpression(Expression)
	 */
	public ReactiveMongoDbMessageSourceSpec collectionNameExpression(Expression collectionNameExpression) {
		this.target.setCollectionNameExpression(collectionNameExpression);
		return this;
	}

	/**
	 * Configure a custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb. Only allowed if this instance was constructed with a
	 * {@link ReactiveMongoDatabaseFactory}.
	 * @param mongoConverter The mongo converter.
	 * @return the spec
	 * @see ReactiveMongoDbMessageSource#setMongoConverter(MongoConverter)
	 */
	public ReactiveMongoDbMessageSourceSpec mongoConverter(MongoConverter mongoConverter) {
		this.target.setMongoConverter(mongoConverter);
		return this;
	}

}
