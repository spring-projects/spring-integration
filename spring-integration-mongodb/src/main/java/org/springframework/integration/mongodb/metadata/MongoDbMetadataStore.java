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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ScriptOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.script.ExecutableMongoScript;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

import com.mongodb.BasicDBObject;

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
	private static final String DEFAULT_COLLECTION_NAME = "metadataStore";
	private final String collectionName;
	private static final String ID_FIELD = "_id";
	private static final String VALUE = "value";

	/**
	 *
	 * Configure the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 *
	 * @param template
	 *            The mongodb template
	 * @param collectionName
	 *            the collection name where it persists the data
	 */
	public MongoDbMetadataStore(MongoTemplate template, String collectionName) {
		Assert.notNull(template, "'template' must not be null.");
		Assert.hasText(collectionName, "'collectionName' must not be empty.");
		this.template = template;
		this.collectionName = collectionName;
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoTemplate} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 *
	 * @param template
	 *            The mongodb template
	 */
	public MongoDbMetadataStore(MongoTemplate template) {
		this(template, DEFAULT_COLLECTION_NAME);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * default collection name - {@link #DEFAULT_COLLECTION_NAME}.
	 *
	 * @param factory
	 *            The mongodb factory
	 */
	public MongoDbMetadataStore(MongoDbFactory factory) {
		this(factory, DEFAULT_COLLECTION_NAME);
	}

	/**
	 * Configure the MongoDbMetadataStore by provided {@link MongoDbFactory} and
	 * collection name
	 *
	 * @param collectionName
	 *            the collection name where it persists the data
	 * @param factory
	 *            MongoDbFactory
	 */
	public MongoDbMetadataStore(MongoDbFactory factory, String collectionName) {
		this(new MongoTemplate(factory), collectionName);
	}

	/**
	 *
	 * @param key
	 *            Its metadata key usually filename
	 * @param value
	 *            Its a metadata value
	 * @see org.springframework.integration.metadata.MetadataStore#put(String,
	 *      String)
	 */
	@Override
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		final Map<String, String> entry = new HashMap<String, String>();
		entry.put(ID_FIELD, key);
		entry.put(VALUE, value);
		template.execute(collectionName, (CollectionCallback<Object>) collection -> collection
				.save(new BasicDBObject(entry)));
	}

	/**
	 * @param key
	 *            Its metadata key usually filename
	 * @return String
	 * @see org.springframework.integration.metadata.MetadataStore#get(String)
	 */
	@Override
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		query.fields().exclude(ID_FIELD);
		@SuppressWarnings("unchecked")
		Map<String, String> result = template.findOne(query, Map.class, collectionName);
		return result == null ? null : result.get(VALUE);

	}

	/**
	 * @param key
	 *            Its metadata key usually filename
	 * @return String
	 * @see org.springframework.integration.metadata.MetadataStore#remove(String)
	 */
	@Override
	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key));
		query.fields().exclude(ID_FIELD);
		@SuppressWarnings("unchecked")
		Map<String, String> result = template.findAndRemove(query, Map.class, collectionName);
		return result == null ? null : result.get(VALUE);
	}

	/**
	 * @param key
	 *            Its metadata key usually filename
	 * @param value
	 *            String
	 * @return String
	 * @see org.springframework.integration.metadata.ConcurrentMetadataStore#putIfAbsent(String,
	 *      String)
	 */
	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		ScriptOperations operations = template.scriptOps();
		ExecutableMongoScript script = new ExecutableMongoScript(buildQuery());
		operations.register(script);
		BasicDBObject result = (BasicDBObject) operations.execute(script, key, value);
		return (result == null) ? null : (String) result.get(VALUE);
	}

	/**
	 * @param key
	 *            Its metadata key usually filename
	 * @param oldValue
	 *            String
	 * @param newValue
	 *            String
	 * @return boolean
	 * @see org.springframework.integration.metadata.ConcurrentMetadataStore#replace(String,
	 *      String, String)
	 */
	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		Query query = new Query(Criteria.where(ID_FIELD).is(key).and(VALUE).is(oldValue));
		return template.updateFirst(query, Update.update(VALUE, newValue), collectionName)
				.isUpdateOfExisting();
	}

	private String buildQuery() {
		return "function putIfAbsent(key,value){ " + " key = key.substring(1,key.length-1);"
				+ " value = value.substring(1,value.length-1);" + " var alreadyPresent = db."
				+ collectionName + ".find({\"_id\":key.toString()},{\"_id\":0}); "
				+ "if(alreadyPresent.length() < 1){ " + "   db." + collectionName + ".insert({"
				+ "\"_id\":key.toString(),\"value\":value.toString()}); " + "  return null; "
				+ "} " + "return alreadyPresent.toArray()[0]; " + "} ";
	}

}
