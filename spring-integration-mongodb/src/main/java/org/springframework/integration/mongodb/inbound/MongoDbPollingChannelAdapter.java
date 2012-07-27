/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.mongodb.inbound;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The polling channel adapter that is used to get data from the Mongo Data source
 * by executing the provided Query. An optional update can be executed after the select.
 *
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbPollingChannelAdapter extends IntegrationObjectSupport
		implements MessageSource<Object>, BeanClassLoaderAware {

	private MongoDbQueryFactory queryFactory;

	private MongoTemplate mongoTemplate;

	private final String collection;

	private final MongoDbFactory factory;

	private Query query;

	private Update update;

	private final MessageReadingMongoConverter converter;

	private Class<?> entityClass = MessageWrapper.class;

	/**
	 * @param collection
	 * @param factory
	 */
	public MongoDbPollingChannelAdapter(MongoDbFactory factory,String collection) {
		this.factory = factory;
		this.collection = collection;
		converter = new MessageReadingMongoConverter(factory, new MongoMappingContext());
	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		converter.setBeanClassLoader(classLoader);
	}



	/* (non-Javadoc)
	 * @see org.springframework.integration.core.MessageSource#receive()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Message<Object> receive() {
		Query select;
		if(query != null) {
			select = query;
		}
		else {
			select = queryFactory.getQuery();
			Assert.notNull(select, "Provided Query instance is null");
		}

		Update update;
		if(queryFactory.isUpdateStatic()) {
			update = this.update;
		}
		else {
			update = queryFactory.getUpdate();
		}

		//TODO: Support deletion after read??

		Message message = null;
		Object result;
		if(update == null) {
			if(StringUtils.hasText(collection)) {
				result = mongoTemplate.findOne(select, entityClass, collection);
			}
			else {
				result = mongoTemplate.findOne(select,entityClass);
			}
		}
		else {
			//By default FindAndModifyOptions will have returnNew as false and hence will
			//return the old document
			if(StringUtils.hasText(collection)) {
				result = mongoTemplate.findAndModify(select, update, entityClass, collection);
			}
			else {
				result = mongoTemplate.findAndModify(select, update, entityClass);
			}
		}

		if(result != null) {
			if(result instanceof Message) {
				message = (Message)result;
			}
			else if(result instanceof MessageWrapper){
				message = ((MessageWrapper)result).getMessage();
			}
			else {
				message = MessageBuilder.withPayload(result).build();
			}
		}

		return message;
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(queryFactory,"MongoDbQueryFactory provided is null");
		if(queryFactory.isStaticQuery()) {
			query = queryFactory.getQuery();
			Assert.notNull(query,"Provided Query instance is null");
		}

		if(queryFactory.isUpdateStatic()) {
			update = queryFactory.getUpdate();
		}

		converter.afterPropertiesSet();
		//lets create the mongo template instance
		mongoTemplate = new MongoTemplate(factory, converter);
	}

	/**
	 * Sets the {@link MongoDbQueryFactory} instance that would be used to get the {@link Query}
	 * instance to be executed for receiving the data from Mongo DB.
	 *
	 * @param queryFactory
	 */
	public void setQueryFactory(MongoDbQueryFactory queryFactory) {
		Assert.notNull(queryFactory,"A non null query factory instance is expected");
		this.queryFactory = queryFactory;
	}


	/**
	 * The entity class the adapter expects
	 * @param entityClass
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass,"A non null resultClass is expected");
		this.entityClass = entityClass;
	}
}
