/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ScriptOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.script.NamedMongoScript;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;

/**
 * MongoDbMetadataStore implementation of {@link ConcurrentMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore} to
 * achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author Senthil Arumugam, Samiraj Panneer Selvam
 * @author Artem Bilan
 * @since 4.2
 *
 */
public class MongoDbMetadataStore implements ConcurrentMetadataStore {

	private static final String DEFAULT_COLLECTION_NAME = "metadataStore";

	private static final String ID_FIELD = "_id";

	private static final String VALUE = "value";

	private static final String PUT_IF_ABSENT_FUNCTION =
			"function putIfAbsent(collection, key, value){ " +
					"  var alreadyPresent = db[collection].findOne({\"_id\": key}, {\"_id\": 0}); " +
					"  if(alreadyPresent == null){" +
					"      db[collection].insert({\"_id\": key, \"value\": value}); " +
					"      return null; " +
					"   }" +
					"   return alreadyPresent;" +
					"}";

	private static final String PUT_IF_ABSENT_SCRIPT_NAME = "metadataStorePutIfAbsent";

	private final MongoTemplate template;

	private final String collectionName;

	private final ScriptOperations scriptOperations;

	private volatile boolean scriptInitialized;

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 * @param factory the mongodb factory
	 */
	public MongoDbMetadataStore(MongoDbFactory factory) {
		this(factory, DEFAULT_COLLECTION_NAME);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * collection name
	 * @param factory the mongodb factory
	 * @param collectionName the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoDbFactory factory, String collectionName) {
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
		this.scriptOperations = template.scriptOps();
	}

	/**
	 * Store a metadata {@code value} under provided {@code key} to the configured
	 * {@link #collectionName}.
	 * <p>
	 * If a document does not exist with the specified {@code key}, the method performs an {@code insert}.
	 * If a document exists with the specified {@code key}, the method performs an {@code update}.
	 * @param key the metadata entry key
	 * @param value the metadata entry value
	 * @see MongoTemplate#execute(String, CollectionCallback)
	 * @see DBCollection#save
	 */
	@Override
	public void put(String key, String value) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(value, "'value' must not be empty.");
		final Map<String, String> entry = new HashMap<String, String>();
		entry.put(ID_FIELD, key);
		entry.put(VALUE, value);
		this.template.execute(this.collectionName, (CollectionCallback<Object>) new CollectionCallback<Object>() {

			@Override
			public Object doInCollection(DBCollection collection) throws MongoException, DataAccessException {
				return collection.save(new BasicDBObject(entry));
			}

		});
	}

	/**
	 * Get the {@code value} for the provided {@code key} performing {@code findOne} MongoDB operation.
	 * @param key the metadata entry key
	 * @return the metadata entry value or null if doesn't exist.
	 * @see MongoTemplate#findOne(Query, Class, String)
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty.");
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
		Assert.hasText(key, "'key' must not be empty.");
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
	 * <p>
	 * Performs the {@code stored} JavaScript function.
	 * @param key the metadata entry key
	 * @param value the metadata entry value to store
	 * @return null if successful, the old value otherwise.
	 * @see java.util.concurrent.ConcurrentMap#putIfAbsent(Object, Object)
	 * @see ScriptOperations#call(String, Object...)
	 */
	@Override
	public String putIfAbsent(String key, String value) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(value, "'value' must not be empty.");
		if (!this.scriptInitialized) {
			synchronized (this) {
				if (!this.scriptInitialized) {
					this.scriptOperations.register(
							new NamedMongoScript(PUT_IF_ABSENT_SCRIPT_NAME, PUT_IF_ABSENT_FUNCTION));
					this.scriptInitialized = true;
				}
			}
		}
		BasicDBObject result =
				(BasicDBObject) this.scriptOperations.call(PUT_IF_ABSENT_SCRIPT_NAME, this.collectionName, key, value);
		return (result == null) ? null : (String) result.get(VALUE);
	}

	/**
	 * Replace an existing metadata entry {@code value} with a new one. Otherwise does nothing.
	 * Performs {@code updateFirst} if a document for the provided {@code key} and {@code oldValue}
	 * exists in the {@link #collectionName}.
	 * @param key the metadata entry key
	 * @param oldValue the metadata entry old value to replace
	 * @param newValue the metadata entry new value to put
	 * @return {@code true} if replace was successful, {@code false} otherwise.
	 * @see MongoTemplate#updateFirst(Query, Update, String)
	 */
	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(oldValue, "'oldValue' must not be empty.");
		Assert.hasText(newValue, "'newValue' must not be empty.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key).and(VALUE).is(oldValue));
		return this.template.updateFirst(query, Update.update(VALUE, newValue), this.collectionName)
				.isUpdateOfExisting();
	}

}
