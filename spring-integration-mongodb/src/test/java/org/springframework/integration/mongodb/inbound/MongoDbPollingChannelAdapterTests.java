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

import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME;

import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
import org.springframework.integration.mongodb.Person;
import org.springframework.integration.mongodb.outbound.MongoDbMessageHandler;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

import com.mongodb.WriteConcern;

/**
 * The test case for the inbound {@link MongoDbPollingChannelAdapter}
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbPollingChannelAdapterTests extends MongoDbAvailableTests {


	/**
	 * Tests by providing a null MongoDB Factory
	 *
	 */
	@Test
	public void withNullMongoDBFactory() {
		try {
			@SuppressWarnings("unused")
			MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(null,null,"{}");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Non null MongoDBFactory instance expected", e.getMessage());
			return;
		}
		Assert.assertTrue("Expected to catch an exception, but didnt ", false);
	}

	/**
	 * Case that will test when both the update query and delete after select are set to true
	 * @throws Exception
	 */
	@Test
	@MongoDbAvailable
	public void withQueryUpdateAndDeleteAfterSelect() throws Exception {
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(prepareMongoFactory(),DEFAULT_COLLECTION_NAME,"{}","{}");
		adapter.setDeleteAfterSelect(true);
		try {
			adapter.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Only one of update of delete after select may be specified", e.getMessage());
		}

	}

	/**
	 * Test case when a null select query is provided
	 *
	 * @throws Exception
	 */
	@Test
	@MongoDbAvailable
	public void withNullStaticQuery() throws Exception {
		try {
			MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
					prepareMongoFactory(),null,null);
			adapter.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The Select query is mandatory",
					e.getMessage());
		}
	}

	@Test
	public void withNegativeLimitValue() throws Exception {
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(),null,"{}");
		try {
			adapter.setLimit(-1);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Limit value should be greater than 0", e.getMessage());
		}
	}

	@Test
	@MongoDbAvailable
	public void withLimitGreaterThanAvailableRecords() throws Exception {
		setupPersonDocuments();
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(),"person","{}");
		adapter.setSortOn("age");
		adapter.setSortOrder(Order.DESCENDING);
		adapter.setEntityClass(Person.class);
		adapter.setLimit(10);
		adapter.afterPropertiesSet();
		String[] names = {"Person Three","Person Two","Person One"};
		@SuppressWarnings("unchecked")
		BlockingQueue<Object> queue =
			TestUtils.getPropertyValue(adapter, "retrievedObjects", BlockingQueue.class);
		for(int i = 0;i < 3;i++) {
			Message<Object> object = adapter.receive();
			Assert.assertEquals(2 - i, queue.size());
			Object payload = object.getPayload();
			Assert.assertNotNull("Expecting a non null person object",payload);
			Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
			Assert.assertEquals(names[i],((Person)payload).getName());
		}
	}

	@Test
	@MongoDbAvailable
	public void withLimitsLessThanMaxRecords() throws Exception {
		setupPersonDocuments();
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(),"person","{}");
		adapter.setSortOn("age");
		adapter.setSortOrder(Order.DESCENDING);
		adapter.setEntityClass(Person.class);
		adapter.setLimit(2);
		adapter.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		BlockingQueue<Object> queue =
			TestUtils.getPropertyValue(adapter, "retrievedObjects", BlockingQueue.class);
		Message<Object> object = adapter.receive();
		Assert.assertEquals(1, queue.size());
		Object payload = object.getPayload();
		Assert.assertNotNull("Expecting a non null person object",payload);
		Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
	}


	/**
	 * Reads from the collection and then updates the read document
	 * @throws Exception
	 */
	//@Test
	@MongoDbAvailable
	public void withFindAndUpdate() throws Exception {
		//TODO: This test doesnt work when uncommented. Basically any update
		//that has the nested JSON document doesnt work as we have our custom
		//DBObject to UUID converter which misbehaves. Using the Update instance
		//as below works though as the DBObject generated by both methods is
		//different. In one the value is another DBObject where as in another it is a Map

//		Update update = new Update().set("read", true);
//		Query query = new Query().addCriteria(new Criteria("read").exists(false));
		// first clear the Person collection
		setupPersonDocuments();
		//Below doesnt work
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "person",
				"{\"read\":{\"$exists\":false}}",
				"{\"$set\":{\"read\":true}}");

		//Where as this works
//		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
//				prepareMongoFactory(), "person",
//				query,
//				update);
		adapter.setSortOn("age");
		adapter.setSortOrder(Order.DESCENDING);
		adapter.setEntityClass(Person.class);
		adapter.afterPropertiesSet();
		String[] names = {"Person Three","Person Two","Person One"};
		for(int i = 0;i < 3;i++) {
			Message<Object> object = adapter.receive();
			Object payload = object.getPayload();
			Assert.assertNotNull("Expecting a non null person object",payload);
			Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
			Assert.assertEquals(names[i],((Person)payload).getName());
			Thread.sleep(10);

		}
		Assert.assertNull(adapter.receive());
	}

	/**
	 * @throws Exception
	 */
	private void setupPersonDocuments() throws Exception {
		MongoDbFactory factory = prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(factory);
		template.setWriteConcern(WriteConcern.FSYNC_SAFE);
		template.remove(new Query(), "person");
		Person person = new Person("Person One", 20);
		template.insert(person);
		person = new Person("Person Two", 25);
		template.insert(person);
		person = new Person("Person Three", 30);
		template.insert(person);
	}

	@Test
	@MongoDbAvailable
	public void withFindAndDelete() throws Exception {
		setupPersonDocuments();
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "person",
				"{\"read\":{\"$exists\":false}}");
		adapter.setSortOn("age");
		adapter.setSortOrder(Order.ASCENDING);
		adapter.setEntityClass(Person.class);
		adapter.setDeleteAfterSelect(true);
		adapter.afterPropertiesSet();

		String[] names = {"Person One","Person Two","Person Three"};
		for(int i = 0;i < 3;i++) {
			Message<Object> object = adapter.receive();
			Assert.assertNotNull("Expected a non null object", object);
			Object payload = object.getPayload();
			Assert.assertNotNull("Expecting a non null Person object",payload);
			Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
			Assert.assertEquals(names[i], ((Person)payload).getName());
			Thread.sleep(10);
		}
		Assert.assertNull(adapter.receive());
	}


	@Test
	@MongoDbAvailable
	public void withMessageFromOutboundAdapter() throws Exception {
		MongoDbFactory factory = prepareMongoFactory();
		String collection = "MessageWrapper";

		//clear the collection first
		MongoTemplate template = new  MongoTemplate(factory);
		template.setWriteConcern(WriteConcern.FSYNC_SAFE);
		template.remove(new Query(), collection);

		//send two messages using the outbound adapter
		MongoDbMessageHandler handler = new MongoDbMessageHandler(factory, collection);
		handler.afterPropertiesSet();
		handler.handleMessage(MessageBuilder.withPayload("String One").build());
		handler.handleMessage(MessageBuilder.withPayload("String Two").build());


		MongoDbPollingChannelAdapter inbound = new MongoDbPollingChannelAdapter(factory, collection,
				"{}");
		inbound.setDeleteAfterSelect(true);
		inbound.setSortOn(MongoDbIntegrationConstants.CREATED_DATE);
		inbound.setSortOrder(Order.ASCENDING);
		inbound.afterPropertiesSet();

		String[] payloads = {"String One","String Two"};
		for(int i = 0;i < 2;i++) {
			Message<Object> object = inbound.receive();
			Assert.assertNotNull(object);
			Object payload = object.getPayload();
			Assert.assertTrue(String.class.isAssignableFrom(payload.getClass()));
			Assert.assertEquals(payloads[i], (String)payload);
			Thread.sleep(10);
		}

		Message<Object> object = inbound.receive();
		Assert.assertNull(object);
	}
}