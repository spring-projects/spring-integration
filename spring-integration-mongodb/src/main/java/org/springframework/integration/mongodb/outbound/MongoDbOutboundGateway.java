/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.mongodb.outbound;

import org.bson.Document;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Makes outbound operations to query a MongoDb database using a {@link MongoOperations}
 *
 * @author Xavier Padró
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MongoDbOutboundGateway extends AbstractReplyProducingMessageHandler {

	private MongoDbFactory mongoDbFactory;

	private MongoConverter mongoConverter;

	private MongoOperations mongoTemplate;

	private EvaluationContext evaluationContext;

	private Expression queryExpression;

	@Deprecated
	private CollectionCallback<?> collectionCallback;

	private MessageCollectionCallback<?> messageCollectionCallback;

	private boolean expectSingleResult = false;

	private Class<?> entityClass = Document.class;

	private Expression collectionNameExpression;

	public MongoDbOutboundGateway(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, new MappingMongoConverter(new DefaultDbRefResolver(mongoDbFactory),
				new MongoMappingContext()));
	}

	public MongoDbOutboundGateway(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null.");
		Assert.notNull(mongoConverter, "mongoConverter must not be null.");
		this.mongoDbFactory = mongoDbFactory;
		this.mongoConverter = mongoConverter;
	}

	public MongoDbOutboundGateway(MongoOperations mongoTemplate) {
		Assert.notNull(mongoTemplate, "mongoTemplate must not be null.");
		this.mongoTemplate = mongoTemplate;
	}

	public void setQueryExpression(Expression queryExpression) {
		Assert.notNull(queryExpression, "queryExpression must not be null.");
		this.queryExpression = queryExpression;
	}

	public void setQueryExpressionString(String queryExpressionString) {
		Assert.notNull(queryExpressionString, "queryExpressionString must not be null.");
		this.queryExpression = EXPRESSION_PARSER.parseExpression(queryExpressionString);
	}

	/**
	 * Specify a {@link CollectionCallback} to perform against MongoDB collection.
	 * @param collectionCallback the callback to perform against MongoDB collection.
	 * @deprecated in favor of {{@link #setMessageCollectionCallback(MessageCollectionCallback)}}.
	 * Will be removed in 5.2
	 */
	@Deprecated
	public void setCollectionCallback(CollectionCallback<?> collectionCallback) {
		Assert.notNull(collectionCallback, "'collectionCallback' must not be null.");
		this.collectionCallback = collectionCallback;
	}

	/**
	 * Specify a {@link MessageCollectionCallback} to perform against MongoDB collection
	 * in the request message context.
	 * @param messageCollectionCallback the callback to perform against MongoDB collection.
	 * @since 5.0.11
	 */
	public void setMessageCollectionCallback(MessageCollectionCallback<?> messageCollectionCallback) {
		Assert.notNull(messageCollectionCallback, "'messageCollectionCallback' must not be null.");
		this.messageCollectionCallback = messageCollectionCallback;
	}

	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "entityClass must not be null.");
		this.entityClass = entityClass;
	}

	public void setCollectionNameExpression(Expression collectionNameExpression) {
		Assert.notNull(collectionNameExpression, "collectionNameExpression must not be null.");
		this.collectionNameExpression = collectionNameExpression;
	}

	public void setCollectionNameExpressionString(String collectionNameExpressionString) {
		Assert.notNull(collectionNameExpressionString, "collectionNameExpressionString must not be null.");
		this.collectionNameExpression = EXPRESSION_PARSER.parseExpression(collectionNameExpressionString);
	}

	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.notNull(mongoConverter, "mongoConverter cannot be null");
		Assert.isNull(this.mongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with MongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void doInit() {
		Assert.state(this.queryExpression != null ||
						this.collectionCallback != null ||
						this.messageCollectionCallback != null,
				"no query or collectionCallback is specified");
		Assert.state(this.collectionNameExpression != null, "no collection name specified");
		if (this.queryExpression != null &&
				(this.collectionCallback != null || this.messageCollectionCallback != null)) {
			throw new IllegalStateException("query and collectionCallback are mutually exclusive");
		}

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());

			TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
			if (typeLocator instanceof StandardTypeLocator) {
				((StandardTypeLocator) typeLocator).registerImport(Query.class.getPackage().getName());
			}
		}

		if (this.mongoTemplate == null) {
			this.mongoTemplate = new MongoTemplate(this.mongoDbFactory, this.mongoConverter);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String collectionName =
				this.collectionNameExpression.getValue(this.evaluationContext, requestMessage, String.class);
		// TODO: 5.2 assert not null
		Object result;

		if (this.collectionCallback != null) {
			result = this.mongoTemplate.execute(collectionName, this.collectionCallback); // NOSONAR
		}
		else if (this.messageCollectionCallback != null) {
			result = this.mongoTemplate.execute(collectionName, // NOSONAR
					collection -> this.messageCollectionCallback.doInCollection(collection, requestMessage));
		}
		else {
			Query query = buildQuery(requestMessage);

			if (this.expectSingleResult) {
				result = this.mongoTemplate.findOne(query, this.entityClass, collectionName); // NOSONAR
			}
			else {
				result = this.mongoTemplate.find(query, this.entityClass, collectionName); // NOSONAR
			}
		}

		return result;
	}

	private Query buildQuery(Message<?> requestMessage) {
		Query query;

		Object expressionValue =
				this.queryExpression.getValue(this.evaluationContext, requestMessage, Object.class);

		if (expressionValue instanceof String) {
			query = new BasicQuery((String) expressionValue);
		}
		else if (expressionValue instanceof Query) {
			query = ((Query) expressionValue);
		}
		else {
			throw new IllegalStateException("'queryExpression' must evaluate to " +
					"String or org.springframework.data.mongodb.core.query.Query");
		}

		return query;
	}

}
