/*
 * Copyright 2007-2012 the original author or authors
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
package org.springframework.integration.mongodb.inbound;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.mongodb.DBObject;

/**
 * An instance of {@link MessageSource} which returns a {@link Message} with payload
 * which is a result of the execution of {@link Query}.
 * MongoDb {@link Query} is executed using {@link MongoOperations#find(Query, Class)}
 * method which returns a {@link List}. The returned {@link List} will be used as
 * a payoad of the {@link Message} returned by the {{@link #receive()} method
 * Empty {@link List} is treated as null, thus resulting in no {@link Message} returned
 * by the {{@link #receive()} method
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 */
public class MongoDbMessageSource extends IntegrationObjectSupport
				implements PseudoTransactionalMessageSource<Object, MongoOperations>{

	private final Expression queryExpression;

	private volatile Expression collectionNameExpression = new LiteralExpression("data");

	private volatile StandardEvaluationContext evaluationContext;

	private volatile MongoOperations mongoTemplate;

	private volatile MongoConverter mongoConverter;

	private volatile MongoDbFactory mongoDbFactory;

	private volatile boolean initialized = false;

	private volatile Class<?> entityClass = DBObject.class;

	private volatile boolean expectSingleResult = false;

	/**
	 * Creates this instance with provided {@link MongoDbFactory} and SpEL expression
	 * which should resolve to a MongoDb 'query' string
	 * (see http://www.mongodb.org/display/DOCS/Querying)
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 *
	 * @param mongoDbFactory
	 * @param queryExpression
	 */
	public MongoDbMessageSource(MongoDbFactory mongoDbFactory, Expression queryExpression){
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");

		this.mongoDbFactory = mongoDbFactory;
		this.queryExpression = queryExpression;
	}

	/**
	 * Creates this instance with provided {@link MongoOperations} and SpEL expression
	 * which should resolve to a Mongo 'query' string
	 * (see http://www.mongodb.org/display/DOCS/Querying)
	 * It assumes that {@link MongoOperations} is fully initialized and ready to be used.
	 * The 'queryExpression' will be evaluated on every call to the {@link #receive()} method.
	 *
	 * @param mongoTemplate
	 * @param queryExpression
	 */
	public MongoDbMessageSource(MongoOperations mongoTemplate, Expression queryExpression){
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");
		Assert.notNull(queryExpression, "'queryExpression' must not be null");

		this.mongoTemplate = mongoTemplate;
		this.queryExpression = queryExpression;
	}

	/**
	 * Allows you to set the type of the entityClass that will be passed to
	 * {@link MongoTemplate#find(Query, Class)} or {@link MongoTemplate#findOne(Query, Class)}
	 * method;
	 * Default is {@link DBObject}
	 *
	 * @param entityClass
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	/**
	 * Allows you to manage which find* method to invoke on {@link MongoTemplate}
	 * Default is 'false' which means {@link #receive()} method will use
	 * {@link MongoTemplate#find(Query, Class)} method. If set to 'true'
	 * {@link #receive()} will use {@link MongoTemplate#findOne(Query, Class)}
	 * and the payload of the returned {@link Message} will be the returned target Object of type
	 * identified by {{@link #entityClass} instead of a List.
	 *
	 * @param expectSingleResult
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Sets the SpEL {@link Expression} that should resolve to a collection name
	 * used by {@link Query}
	 *
	 * @param collectionNameExpression
	 */
	public void setCollectionNameExpression(Expression collectionNameExpression) {
		this.collectionNameExpression = collectionNameExpression;
	}

	/**
	 * Allows you to provide custom {@link MongoConverter} used to assist in deserialization
	 * data read from MongoDb
	 *
	 * @param mongoConverter
	 */
	public void setMongoConverter(MongoConverter mongoConverter) {
		this.mongoConverter = mongoConverter;
	}


	/**
	 * Will execute {@link Query} returning its results as Message payload.
	 * Payload can be either {@link List} of elements of objects of type
	 * identified by {{@link #entityClass} or a single element of type identified by {{@link #entityClass}
	 * based on the value of {{@link #expectSingleResult} attribute which defaults to 'false' resulting
	 * {@link Message} with payload of type {@link List}
	 */
	public Message<Object> receive() {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Message<Object> message = null;
		Query query = new BasicQuery(this.queryExpression.getValue(this.evaluationContext, String.class));
		String collectionName = this.collectionNameExpression.getValue(this.evaluationContext, String.class);

		Object result = null;
		if (this.expectSingleResult){
			result = mongoTemplate.
					findOne(query, this.entityClass, collectionName);
		}
		else {
			List<?> results = mongoTemplate.
					find(query, this.entityClass, collectionName);
			if (!CollectionUtils.isEmpty(results)){
				result = results;
			}
		}
		if (result != null){
			message = MessageBuilder.withPayload(result).setHeader("collectionName", collectionName).build();
		}

		return message;
	}



	/**
	 * Will return the result object returned as a payload during the call
	 * to {@link #receive()}
	 */
	public MongoOperations getResource() {
		return this.mongoTemplate;
	}

	public void afterCommit(Object object) {

	}

	public void afterRollback(Object object) {

	}

	public void afterReceiveNoTx(MongoOperations resource) {

	}

	public void afterSendNoTx(MongoOperations resource) {

	}
	@Override
	protected void onInit() throws Exception {
		if (this.getBeanFactory() != null){
			this.evaluationContext =
					ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		}
		else {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		}

		if (this.mongoTemplate == null){
			this.mongoTemplate = new MongoTemplate(mongoDbFactory, mongoConverter);
		}
		this.initialized = true;
	}
}
