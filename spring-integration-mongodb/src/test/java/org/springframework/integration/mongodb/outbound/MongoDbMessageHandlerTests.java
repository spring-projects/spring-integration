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
package org.springframework.integration.mongodb.outbound;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.WriteConcern;


/**
 * Test class for the {@link MongoDbMessageHandler} class
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbMessageHandlerTests extends MongoDbAvailableTests {


	/**
	 * Tests by providing a null MongoDB Factory
	 *
	 */
	@Test
	public void withNullMongoDBFactory() {
		try {
			@SuppressWarnings("unused")
			MongoDbMessageHandler handler = new MongoDbMessageHandler(null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("'factory' must not be null", e.getMessage());
			return;
		}
		Assert.assertTrue("Expected to catch an exception, but didnt ", false);
	}

	@Test
	@MongoDbAvailable
	public void sendMessageWithCollectionProvided() throws Exception {
		MongoDbFactory factory = prepareMongoFactory();
		//first clear the collection
		DB db = factory.getDb();
		DBCollection coll = db.getCollection("collection");
		coll.remove(new BasicDBObject(), WriteConcern.FSYNC_SAFE);

		MongoDbMessageHandler handler = new MongoDbMessageHandler(prepareMongoFactory(), "collection");
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload("Hello").build());
		//just want to give sufficient time for the write to be successful
		try {
			Thread.sleep(50);
		} catch (Exception e) {

		}
		DBCursor cursor = coll.find();
		Assert.assertEquals(1, cursor.count());
	}

	@Test
	@MongoDbAvailable
	public void sendMessageWithDefaultCollection() throws Exception {
		MongoDbFactory factory = prepareMongoFactory();
		//first clear the collection
		DB db = factory.getDb();
		DBCollection coll = db.getCollection(MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME);
		coll.remove(new BasicDBObject(), WriteConcern.FSYNC_SAFE);

		MongoDbMessageHandler handler = new MongoDbMessageHandler(prepareMongoFactory());
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload("Hello").build());
		try {
			Thread.sleep(50);
		} catch (Exception e) {

		}
		DBCursor cursor = coll.find();
		Assert.assertEquals(1, cursor.count());
	}
}
