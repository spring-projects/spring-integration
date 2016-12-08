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

package org.springframework.integration.mongodb.outbound;

import org.bson.Document;

import org.springframework.data.mongodb.MongoDbFactory;
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
 * @since 5.0
 */
public class MongoDbOutboundGateway extends AbstractReplyProducingMessageHandler {

	private volatile EvaluationContext evaluationContext;

	private volatile Expression queryExpression;

	private volatile MongoOperations mongoTemplate;

	private volatile MongoDbFactory mongoDbFactory;

	private volatile MongoConverter mongoConverter;

	private volatile boolean expectSingleResult = false;

	private volatile Class<?> entityClass = Document.class;

	private volatile Expression collectionNameExpression;

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
		this.queryExpression = queryExpression;
	}

	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "entityClass must not be null.");
		this.entityClass = entityClass;
	}

	public void setCollectionNameExpression(Expression collectionNameExpression) {
		this.collectionNameExpression = collectionNameExpression;
	}

	public void setMongoConverter(MongoConverter mongoConverter) {
		Assert.notNull(mongoConverter, "mongoConverter cannot be null");
		Assert.isNull(this.mongoTemplate,
				"'mongoConverter' can not be set when instance was constructed with MongoTemplate");
		this.mongoConverter = mongoConverter;
	}

	@Override
	protected void doInit() {
		Assert.isTrue(this.queryExpression != null, "no query specified");
		Assert.isTrue(this.collectionNameExpression != null, "no collection name specified");

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
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String collectionName =
				this.collectionNameExpression.getValue(this.evaluationContext, requestMessage, String.class);
		Query query = buildQuery(requestMessage);

		Object result;

		if (this.expectSingleResult) {
			result = this.mongoTemplate.findOne(query, this.entityClass, collectionName);
		}
		else {
			result = this.mongoTemplate.find(query, this.entityClass, collectionName);
		}

		return result;
	}

	private Query buildQuery(final Message<?> requestMessage) {
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
			throw new IllegalStateException("'queryExpression' must evaluate to String or org.springframework.data.mongodb.core.query.Query");
		}

		return query;
	}
}
