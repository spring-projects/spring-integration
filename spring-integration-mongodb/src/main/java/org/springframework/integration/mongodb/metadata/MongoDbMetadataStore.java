/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.integration.mongodb.metadata;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mongodb.WriteResult;

/**
 * MongoDbMetadataStore implementation of {@link ConcurrentMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore} to
 * achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author Senthil Arumugam, Samiraj Panneer Selvam
 * @since 4.2
 *
 */
public class MongoDbMetadataStore implements ConcurrentMetadataStore {

	protected final Log logger = LogFactory.getLog(getClass());
	private final MongoTemplate template;
	private final String DEFAULT_COLLECTION_NAME = "metadatastore";
	private final String collectionName;
	private final String ID_FIELD = "_id";
	private final String VALUE = "value";
	
	/**
	 * 
	 * Configures the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * 
	 * @param template The mongodb template
	 * @param collectionName the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoTemplate template, String collectionName) {
		this.template = template;
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName
				: DEFAULT_COLLECTION_NAME;
	}

	/**
	 * Configures the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * 
	 * @param template The mongodb template
	 */
	public MongoDbMetadataStore(MongoTemplate template) {
		this(template, null);
	}

	/**
	 * Configures the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * 
	 * @param factory The mongodb factory
	 */
	public MongoDbMetadataStore(MongoDbFactory factory) {
		this(factory, null);
	}
	
	/**
	 * Configures the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * collection name
	 * 
	 * @param collectionName the collection name where it persists the data
	 * @param factory MongoDbFactory
	 */
	public MongoDbMetadataStore(MongoDbFactory factory, String collectionName) {
		template = new MongoTemplate(factory);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName
				: DEFAULT_COLLECTION_NAME;
	}
	
	
	/**
	 * Method put.
	 * @param key String
	 * @param value String
	 * @see org.springframework.integration.metadata.MetadataStore#put(String, String)
	 */
	@Override
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		Map<String,String> fileInfo = new HashMap();
		fileInfo.put(ID_FIELD, key);
		fileInfo.put(VALUE, value);
		template.save(fileInfo, collectionName);
	}

	/**
	 * Method get.
	 * @param key String
	 * @return String
	 * @see org.springframework.integration.metadata.MetadataStore#get(String)
	 */
	@Override
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		@SuppressWarnings("unchecked")
		Map<String,String> result = template.findOne(query, Map.class, collectionName);
		return result==null?null:result.get(VALUE);
		
	}

	/**
	 * Method remove.
	 * @param key String
	 * @return String
	 * @see org.springframework.integration.metadata.MetadataStore#remove(String)
	 */
	@Override
	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		@SuppressWarnings("unchecked")
		Map<String,String> result = template.findAndRemove(query, Map.class,collectionName);
		return result==null?null:result.get(VALUE);
	}

	/**
	 * Method putIfAbsent.
	 * @param key String
	 * @param value String
	 * @return String
	 * @see org.springframework.integration.metadata.ConcurrentMetadataStore#putIfAbsent(String, String)
	 */
	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");

		String result = null;
		Map<String,String> fileInfo = new HashMap();
		fileInfo.put(ID_FIELD, key);
		fileInfo.put(VALUE, value);
		try {
			template.insert(fileInfo, collectionName);
		} catch (DuplicateKeyException e) {
			if(logger.isDebugEnabled()){
				logger.debug("DUPLICATE KEY VIOLOATION",e);
			}
			result = get(key);
		}
		return result;
	}

	/**
	 * Method replace.
	 * @param key String
	 * @param oldValue String
	 * @param newValue String
	 * @return boolean
	 * @see org.springframework.integration.metadata.ConcurrentMetadataStore#replace(String, String, String)
	 */
	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		
		Query query = new Query(Criteria.where(ID_FIELD).is(key).and(VALUE).is(oldValue));
		Update update = new Update();
		update.set(VALUE, newValue);
		WriteResult result = template.updateFirst(query, update, Map.class,collectionName);
		return result.isUpdateOfExisting();
	}

}
