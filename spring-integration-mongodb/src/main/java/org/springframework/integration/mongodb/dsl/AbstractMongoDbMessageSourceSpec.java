/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.mongodb.inbound.AbstractMongoDbMessageSource;

/**
 * A {@link MessageSourceSpec} extension for common MongoDB sources options.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public class AbstractMongoDbMessageSourceSpec<S extends AbstractMongoDbMessageSourceSpec<S, H>,
		H extends AbstractMongoDbMessageSource<?>>
		extends MessageSourceSpec<S, H> {

	/**
	 * Allow you to set the type of the entityClass that will be passed to the the MongoDB query method.
	 * Default is {@link com.mongodb.DBObject}.
	 * @param entityClass The entity class.
	 * @return the spec
	 * @see AbstractMongoDbMessageSource#setEntityClass(Class)
	 */
	public S entityClass(Class<?> entityClass) {
		this.target.setEntityClass(entityClass);
		return _this();
	}

	/**
	 * Allow you to manage which find* method to invoke.
	 * @param expectSingleResult true if a single result is expected.
	 * @return the spec
	 * @see AbstractMongoDbMessageSource#setExpectSingleResult(boolean)
	 */
	public S expectSingleResult(boolean expectSingleResult) {
		this.target.setExpectSingleResult(expectSingleResult);
		return _this();
	}

	/**
	 * Configure a collection name to query against.
	 * @param collectionName the name of the MongoDb collection
	 * @return the spec
	 */
	public S collectionName(String collectionName) {
		return collectionNameExpression(new LiteralExpression(collectionName));
	}

	/**
	 * Configure a SpEL expression to evaluation a collection name on each {@code receive()} call.
	 * @param collectionNameExpression the SpEL expression for name of the MongoDb collection
	 * @return the spec
	 */
	public S collectionNameExpression(String collectionNameExpression) {
		return collectionNameExpression(PARSER.parseExpression(collectionNameExpression));
	}

	/**
	 * Configure a {@link Supplier} to obtain a collection name on each {@code receive()} call.
	 * @param collectionNameSupplier the {@link Supplier} for name of the MongoDb collection
	 * @return the spec
	 */
	public S collectionNameSupplier(Supplier<String> collectionNameSupplier) {
		return collectionNameExpression(new SupplierExpression<>(collectionNameSupplier));
	}

	/**
	 * Configure a SpEL expression to evaluation a collection name on each {@code receive()} call.
	 * @param collectionNameExpression the SpEL expression for name of the MongoDb collection
	 * @return the spec
	 * @see AbstractMongoDbMessageSource#setCollectionNameExpression(Expression)
	 */
	public S collectionNameExpression(Expression collectionNameExpression) {
		this.target.setCollectionNameExpression(collectionNameExpression);
		return _this();
	}

	/**
	 * Configure a custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb.
	 * @param mongoConverter The mongo converter.
	 * @return the spec
	 * @see AbstractMongoDbMessageSource#setMongoConverter(MongoConverter)
	 */
	public S mongoConverter(MongoConverter mongoConverter) {
		this.target.setMongoConverter(mongoConverter);
		return _this();
	}

	/**
	 * Configure a MongoDB update.
	 * @param update the MongoDB update.
	 * @return the spec
	 */
	public S update(String update) {
		return update(new LiteralExpression(update));
	}

	/**
	 * Configure a MongoDB update.
	 * @param update the MongoDB update.
	 * @return the spec
	 */
	public S update(Update update) {
		return update(new ValueExpression<>(update));
	}

	/**
	 * Configure a {@link Supplier} to produce a MongoDB update on each receive call.
	 * @param updateSupplier the {@link Supplier} for MongoDB update.
	 * @return the spec
	 */
	public S updateSupplier(Supplier<Update> updateSupplier) {
		return update(new SupplierExpression<>(updateSupplier));
	}

	/**
	 * Configure a SpEL expression to evaluate a MongoDB update.
	 * @param updateExpression the expression to evaluate a MongoDB update.
	 * @return the spec
	 */
	public S update(Expression updateExpression) {
		this.target.setUpdateExpression(updateExpression);
		return _this();
	}

}
