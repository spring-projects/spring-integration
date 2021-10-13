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

package org.springframework.integration.mongodb.inbound;

import java.util.Collection;
import java.util.Map;

import org.bson.Document;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mongodb.DBObject;

/**
 * An {@link AbstractMessageSource} extension for common MongoDB sources options and support methods.
 *
 * @param <T> The payload type.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public abstract class AbstractMongoDbMessageSource<T> extends AbstractMessageSource<T>
		implements ApplicationContextAware {

	private static final String ID_FIELD = "_id";

	protected final Expression queryExpression; // NOSONAR - final

	private Expression collectionNameExpression = new LiteralExpression("data");

	private MongoConverter mongoConverter;

	private Class<?> entityClass = DBObject.class;

	private boolean expectSingleResult = false;

	private Expression updateExpression;

	private ApplicationContext applicationContext;

	private volatile boolean initialized = false;

	protected AbstractMongoDbMessageSource(Expression queryExpression) {
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		this.queryExpression = queryExpression;
	}

	/**
	 * Set the type of the entityClass that will be passed to the find MongoDb template operation.
	 * Default is {@link DBObject}.
	 * @param entityClass The entity class.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	/**
	 * Manage which find* method to invoke.
	 * Default is 'false', which means the {@link #receive()} method will use
	 * the {@code find()} method. If set to 'true',
	 * {@link #receive()} will use {@code findOne(Query, Class)},
	 * and the payload of the returned {@link org.springframework.messaging.Message}
	 * will be the returned target Object of type
	 * identified by {@link #entityClass} instead of a List.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Set the SpEL {@link Expression} that should resolve to a collection name
	 * used by the {@link Query}. The resulting collection name will be included
	 * in the {@link org.springframework.integration.mongodb.support.MongoHeaders#COLLECTION_NAME} header.
	 * @param collectionNameExpression The collection name expression.
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "'collectionNameExpression' must not be null");
		this.collectionNameExpression = collectionNameExpression;
	}

	/**
	 * Provide a custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb.
	 * @param mongoConverter The mongo converter.
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		this.mongoConverter = mongoConverter;
	}

	/**
	 * Specify an optional {@code update} for just polled records from the collection.
	 * @param updateExpression SpEL expression for an
	 * {@link org.springframework.data.mongodb.core.query.UpdateDefinition}.
	 * @since 5.5
	 */
	public void setUpdateExpression(Expression updateExpression) {
		this.updateExpression = updateExpression;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public Expression getCollectionNameExpression() {
		return this.collectionNameExpression;
	}

	public MongoConverter getMongoConverter() {
		return this.mongoConverter;
	}

	public Class<?> getEntityClass() {
		return this.entityClass;
	}

	public boolean isExpectSingleResult() {
		return this.expectSingleResult;
	}

	public Expression getUpdateExpression() {
		return this.updateExpression;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	protected boolean isInitialized() {
		return this.initialized;
	}

	@Override
	protected void onInit() {
		super.onInit();
		TypeLocator typeLocator = getEvaluationContext().getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			//Register MongoDB query API package so FQCN can be avoided in query-expression.
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.mongodb.core.query");
		}
	}

	protected Query evaluateQueryExpression() {
		Object value = this.queryExpression.getValue(getEvaluationContext());
		Assert.notNull(value, "'queryExpression' must not evaluate to null");
		Query query = null;
		if (value instanceof String) {
			query = new BasicQuery((String) value);
		}
		else if (value instanceof Query) {
			query = ((Query) value);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to String " +
					"or org.springframework.data.mongodb.core.query.Query, but not: " + query);
		}
		return query;
	}

	protected String evaluateCollectionNameExpression() {
		String collectionName = getCollectionNameExpression().getValue(getEvaluationContext(), String.class);
		Assert.notNull(collectionName, "'collectionNameExpression' must not evaluate to null");
		return collectionName;
	}

	/*
	 * Inspired by {@code org.springframework.data.mongodb.core.EntityOperations#getByIdInQuery}
	 */
	protected Query getByIdInQuery(Collection<?> entities) {
		MultiValueMap<String, Object> byIds = new LinkedMultiValueMap<>();

		entities.stream()
				.map(this::idForEntity)
				.forEach(it -> byIds.add(it.getFirst(), it.getSecond()));

		Criteria[] criterias = byIds.entrySet().stream()
				.map(it -> Criteria.where(it.getKey()).in(it.getValue()))
				.toArray(Criteria[]::new);

		return new Query(criterias.length == 1 ? criterias[0] : new Criteria().orOperator(criterias));
	}

	@SuppressWarnings("unchecked")
	protected Pair<String, Object> idForEntity(Object entity) {
		if (entity instanceof String) {
			return idFieldFromMap(Document.parse(entity.toString()));
		}
		if (entity instanceof Map) {
			return idFieldFromMap((Map<String, Object>) entity);
		}

		MappingContext<? extends MongoPersistentEntity<?>, ?> context = this.mongoConverter.getMappingContext();

		MongoPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(entity.getClass());
		String idField = persistentEntity.getRequiredIdProperty().getFieldName();
		IdentifierAccessor idAccessor = persistentEntity.getIdentifierAccessor(entity);
		return Pair.of(idField, idAccessor.getRequiredIdentifier());
	}

	@Nullable
	protected Update evaluateUpdateExpression() {
		if (this.updateExpression != null) {
			Object value = this.updateExpression.getValue(getEvaluationContext());
			Assert.notNull(value, "'updateExpression' must not evaluate to null");
			Update update;
			if (value instanceof String) {
				update = new BasicUpdate((String) value);
			}
			else if (value instanceof Update) {
				update = ((Update) value);
			}
			else {
				throw new IllegalStateException("'updateExpression' must evaluate to String " +
						"or org.springframework.data.mongodb.core.query.Update");
			}
			return update;
		}
		return null;
	}

	private static Pair<String, Object> idFieldFromMap(Map<String, Object> map) {
		return Pair.of(ID_FIELD, map.get(ID_FIELD));
	}

}
