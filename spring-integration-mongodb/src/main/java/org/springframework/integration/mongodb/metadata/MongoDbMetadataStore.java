/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.mongodb.metadata;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

/**
 * MongoDbMetadataStore implementation of {@link ConcurrentMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore} to
 * achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author Senthil Arumugam, Samiraj Panneer Selvam
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 *
 */
public class MongoDbMetadataStore implements ConcurrentMetadataStore {

	private static final String KEY_MUST_NOT_BE_EMPTY = "'key' must not be empty.";

	private static final String DEFAULT_COLLECTION_NAME = "metadataStore";

	private static final String ID_FIELD = "_id";

	private static final String VALUE = "value";

	private final MongoTemplate template;

	private final String collectionName;

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDatabaseFactory} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * @param factory the mongodb factory
	 */
	public MongoDbMetadataStore(MongoDatabaseFactory factory) {
		this(factory, DEFAULT_COLLECTION_NAME);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDatabaseFactory} and
	 * collection name.
	 * @param factory the mongodb factory
	 * @param collectionName the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoDatabaseFactory factory, String collectionName) {
		this(new MongoTemplate(factory), collectionName);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * @param template the mongodb template
	 */
	public MongoDbMetadataStore(MongoTemplate template) {
		this(template, DEFAULT_COLLECTION_NAME);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoTemplate} and collection name.
	 * @param template the mongodb template
	 * @param collectionName the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoTemplate template, String collectionName) {
		Assert.notNull(template, "'template' must not be null.");
		Assert.hasText(collectionName, "'collectionName' must not be empty.");
		this.template = template;
		this.collectionName = collectionName;
	}

	/**
	 * Store a metadata {@code value} under provided {@code key} to the configured
	 * {@link #collectionName}.
	 * <p>
	 * If a document does not exist with the specified {@code key}, the method performs an {@code insert}.
	 * If a document exists with the specified {@code key}, the method performs an {@code update}.
	 * @param key the metadata entry key
	 * @param value the metadata entry value
	 * @see MongoTemplate#execute(String, org.springframework.data.mongodb.core.CollectionCallback)
	 */
	@Override
	public void put(String key, String value) {
		Assert.hasText(key, KEY_MUST_NOT_BE_EMPTY);
		Assert.hasText(value, "'value' must not be empty.");
		final Map<String, Object> entry = new HashMap<>();
		entry.put(ID_FIELD, key);
		entry.put(VALUE, value);
		this.template.save(new Document(entry), this.collectionName);
	}

	/**
	 * Get the {@code value} for the provided {@code key} performing {@code findOne} MongoDB operation.
	 * @param key the metadata entry key
	 * @return the metadata entry value or null if doesn't exist.
	 * @see MongoTemplate#findOne(Query, Class, String)
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, KEY_MUST_NOT_BE_EMPTY);
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		query.fields().exclude(ID_FIELD);
		@SuppressWarnings("unchecked")
		Map<String, String> result = this.template.findOne(query, Map.class, this.collectionName);
		return result == null ? null : result.get(VALUE);

	}

	/**
	 * Remove the metadata entry for the provided {@code key} and return its {@code value}, if any,
	 * using {@code findAndRemove} MongoDB operation.
	 * @param key the metadata entry key
	 * @return the metadata entry value or null if doesn't exist.
	 * @see MongoTemplate#findAndRemove(Query, Class, String)
	 */
	@Override
	public String remove(String key) {
		Assert.hasText(key, KEY_MUST_NOT_BE_EMPTY);
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		query.fields().exclude(ID_FIELD);
		@SuppressWarnings("unchecked")
		Map<String, String> result = this.template.findAndRemove(query, Map.class, this.collectionName);
		return result == null ? null : result.get(VALUE);
	}

	/**
	 * If the specified key is not already associated with a value, associate it with the given value.
	 * This is equivalent to
	 * <pre> {@code
	 * if (!map.containsKey(key))
	 *   return map.put(key, value);
	 * else
	 *   return map.get(key);
	 * }</pre>
	 * except that the action is performed atomically.
	 * @param key the metadata entry key
	 * @param value the metadata entry value to store
	 * @return null if successful, the old value otherwise.
	 * @see java.util.concurrent.ConcurrentMap#putIfAbsent(Object, Object)
	 */
	@Override
	public String putIfAbsent(String key, String value) {
		Assert.hasText(key, KEY_MUST_NOT_BE_EMPTY);
		Assert.hasText(value, "'value' must not be empty.");

		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		query.fields().exclude(ID_FIELD);
		@SuppressWarnings("unchecked")
		Map<String, String> result = this.template.findAndModify(query, new Update().setOnInsert(VALUE, value),
				new FindAndModifyOptions().upsert(true), Map.class, this.collectionName);
		return result == null ? null : result.get(VALUE);
	}

	/**
	 * Replace an existing metadata entry {@code value} with a new one. Otherwise does nothing.
	 * Performs {@code updateFirst} if a document for the provided {@code key} and {@code oldValue}
	 * exists in the {@link #collectionName}.
	 * @param key the metadata entry key
	 * @param oldValue the metadata entry old value to replace
	 * @param newValue the metadata entry new value to put
	 * @return {@code true} if replace was successful, {@code false} otherwise.
	 * @see MongoTemplate#updateFirst(Query, org.springframework.data.mongodb.core.query.UpdateDefinition, String)
	 */
	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.hasText(key, KEY_MUST_NOT_BE_EMPTY);
		Assert.hasText(oldValue, "'oldValue' must not be empty.");
		Assert.hasText(newValue, "'newValue' must not be empty.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key).and(VALUE).is(oldValue));
		return this.template.updateFirst(query, Update.update(VALUE, newValue), this.collectionName)
				.getModifiedCount() > 0;
	}

}
