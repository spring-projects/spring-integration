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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

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

	@Test
	@MongoDbAvailable
	public void testWriteResultCheckingAndConcern() throws Exception {
		MongoDbMessageHandler handler = new MongoDbMessageHandler(prepareMongoFactory(), "collection");
		handler.setWriteConcern(WriteConcern.FSYNC_SAFE);
		handler.setWriteResultChecking(WriteResultChecking.EXCEPTION);
		handler.afterPropertiesSet();
		Assert.assertNotNull(TestUtils.getPropertyValue(handler, "template", MongoTemplate.class));
		Assert.assertEquals(WriteConcern.FSYNC_SAFE,
				TestUtils.getPropertyValue(handler, "template.writeConcern", WriteConcern.class));
		Assert.assertEquals(WriteResultChecking.EXCEPTION,
				TestUtils.getPropertyValue(handler, "template.writeResultChecking", WriteResultChecking.class));
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
		handler.setWriteConcern(WriteConcern.FSYNC_SAFE);
		handler.setWriteResultChecking(WriteResultChecking.EXCEPTION);
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload("Hello").build());

		DBCursor cursor = coll.find();
		Assert.assertEquals(1, cursor.count());
	}

	@Test
	@MongoDbAvailable
	public void sendMessageWithDefaultCollection() throws Exception {
		MongoDbFactory factory = prepareMongoFactory();
		//first clear the collection
		DB db = factory.getDb();
		DBCollection coll = db.getCollection("messageWrapper");
		coll.remove(new BasicDBObject(), WriteConcern.FSYNC_SAFE);

		MongoDbMessageHandler handler = new MongoDbMessageHandler(prepareMongoFactory());
		handler.setWriteConcern(WriteConcern.FSYNC_SAFE);
		handler.setWriteResultChecking(WriteResultChecking.EXCEPTION);
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload("Hello").build());

		DBCursor cursor = coll.find();
		Assert.assertEquals(1, cursor.count());
	}




}
