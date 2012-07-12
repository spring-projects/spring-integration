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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
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


	private final MongoOperations mongoOperations;

	private final String collection;

	private final Query query;

	private final Update update;

	private final MessageReadingMongoConverter converter;

	private volatile Class<?> entityClass = MessageWrapper.class;

	private volatile boolean deleteAfterSelect;

	private volatile Order sortOrder = Order.ASCENDING;

	private volatile String sortOn;

	private volatile int limit = 100;	//By default fetch a maximum of 100 documents

	private final BlockingQueue<Object> retrievedObjects = new LinkedBlockingQueue<Object>();


	/**
	 * @param collection
	 * @param factory
	 * @param select
	 * @param update
	 *
	 */
	public MongoDbPollingChannelAdapter(MongoDbFactory factory,String collection,
							String select,String update) {
		this(factory,collection,new BasicQuery(select),
					StringUtils.hasText(update)?new BasicUpdate(update):null);
	}

	/**
	 *
	 * @param factory
	 * @param collection
	 * @param query
	 * @param update
	 */
	public MongoDbPollingChannelAdapter(MongoDbFactory factory,String collection,
			Query query,Update update) {
		Assert.notNull(factory, "Non null MongoDBFactory instance expected");
		Assert.notNull(query,"The Select query is mandatory");
		converter = new MessageReadingMongoConverter(factory, new MongoMappingContext());
		this.collection = collection != null?collection:MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME;
		converter.afterPropertiesSet();
		mongoOperations = new MongoTemplate(factory, converter);
		this.query = query;
		this.update = update;
	}

	/**
	 * @param factory
	 * @param collection
	 * @param select
	 *
	 */
	public MongoDbPollingChannelAdapter(MongoDbFactory factory,String collection,
							String select) {
		this(factory,collection,select,null);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		converter.setBeanClassLoader(classLoader);
	}



	/* (non-Javadoc)
	 * @see org.springframework.integration.core.MessageSource#receive()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Message<Object> receive() {
		Message message = null;
		Object result;
		if(update == null && !deleteAfterSelect) {
			result = retrievedObjects.poll();
			if(result == null) {
				query.limit(limit);
				List<?> results = mongoOperations.find(query, entityClass, collection);
				retrievedObjects.addAll(results);
				result = retrievedObjects.poll();
			}
		}
		else {
			result = findAndUpdateOrRemove(query, update);
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

	/**	Method that finds a document in the MongoDB and then executes the update
	 * 	or delete after selection.
	 *
	 * 	@param select
	 * 	@param update
	 * 	@return
	 */
	private Object findAndUpdateOrRemove(Query select, Update update) {
		Object result;
		if(deleteAfterSelect) {
			result = mongoOperations.findAndRemove(select, entityClass, collection);
		}
		else {
			FindAndModifyOptions options = new FindAndModifyOptions();
			result = mongoOperations.findAndModify(select, update, options, entityClass, collection);
		}
		return result;
	}


	@Override
	protected void onInit() throws Exception {
		Assert.isTrue(!(update != null && deleteAfterSelect),
				"Only one of update of delete after select may be specified");
		if(StringUtils.hasText(sortOn)) {
			query.sort().on(sortOn, sortOrder);
		}
	}

	/**
	 * The entity class the adapter expects
	 * @param entityClass
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass,"A non null resultClass is expected");
		this.entityClass = entityClass;
	}


	/**
	 *Set this flag to true if you want the record is to be deleted after being selected.
	 *This flag is mutually exclusive to update to be executed after select.
	 *
	 * @param true if the record is to be deleted after being read
	 */
	public void setDeleteAfterSelect(boolean deleteAfterSelect) {
		this.deleteAfterSelect = deleteAfterSelect;
	}

	/**
	 * Sets the sort order to be used with the select queries
	 * @param sortOrder
	 */
	public void setSortOrder(Order sortOrder) {
		Assert.notNull(sortOrder, "Sets the sort order to be used");
		this.sortOrder = sortOrder;
	}

	/**
	 * Sets the key to be used on which the sorting is to be done
	 * @param sortOn
	 */
	public void setSortOn(String sortOn) {
		Assert.notNull(sortOn, "The sort key to be used should not be null");
		this.sortOn = sortOn;
	}

	/**
	 * Sets the max number of documents to be fetched when a find is invoked
	 * @param limit
	 */
	public void setLimit(int limit) {
		Assert.isTrue(limit > 0,"Limit value should be greater than 0");
		this.limit = limit;
	}

}
