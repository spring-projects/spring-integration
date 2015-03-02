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

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MongoDbMetadataStore implementation of {@link ConcurrentMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore} to
 * achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author SenthilArumugam SamirajPanneerSelvam
 * @since 4.0
 *
 */
public class MongoDbMetadataStore implements ConcurrentMetadataStore {

	private MongoTemplate template;
	private final static String DEFAULT_COLLECTION_NAME = "metadatastore";
	private String collectionName;

	/**
	 * 
	 * Configures the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * 
	 * @param template
	 * @param collectionName
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
	 * @param template
	 */
	public MongoDbMetadataStore(MongoTemplate template) {
		this(template, null);
	}

	@Override
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		Query query = new Query(Criteria.where("key").is(key));
		Datastore fileInfo = new Datastore(key, value);
		if (!template.exists(query, collectionName)) {
			template.save(fileInfo, collectionName);
		}
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where("key").is(key));
		return template.findOne(query, Datastore.class, collectionName).value;
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where("key").is(key));
		return template.findAndRemove(query, Datastore.class).value;
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");

		String result = null;
		Query query = new Query(Criteria.where("key").is(key));
		if (template.exists(query, collectionName)) {
			result = value;
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
		Query query = new Query(Criteria.where("file").is(key));
		Update update = new Update();
		update.set(oldValue, newValue);
		return (template.updateFirst(query, update, Datastore.class).getN() > 0);
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
