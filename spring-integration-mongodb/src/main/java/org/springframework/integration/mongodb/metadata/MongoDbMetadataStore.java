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

	private final MongoTemplate template;
	private final static String DEFAULT_COLLECTION_NAME = "metadatastore";
	private final String collectionName;
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
	 * collection name.
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
	 * @param template The mongodb factory
	 * @param collectionName the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoDbFactory factory, String collectionName) {
		template = new MongoTemplate(factory);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName
				: DEFAULT_COLLECTION_NAME;
	}
	
	
	@Override
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		Datastore fileInfo = new Datastore(key, value);
		template.save(fileInfo, collectionName);
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where("key").is(key));
		Datastore result = template.findOne(query, Datastore.class, collectionName);
		if(result != null){
			return result.value;	
		}
		return null;
		
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where("key").is(key));
		Datastore result = template.findAndRemove(query, Datastore.class,collectionName);
		return result==null?null:result.value;
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");

		String result = value;
		Query query = new Query(Criteria.where("key").is(key));
		if (template.exists(query, collectionName)) {
			result = null;
		} else {
			template.save(new Datastore(key, value), collectionName);
		}
		return result;
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		
		Query query = new Query(Criteria.where("key").is(key).and("value").is(oldValue));
		Update update = new Update();
		update.set("value", newValue);
		WriteResult result = template.updateFirst(query, update, Datastore.class,collectionName);
		return result.isUpdateOfExisting();
	}

	private class Datastore {
		String key;
		String value;

		Datastore(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}
