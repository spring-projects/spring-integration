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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
import org.springframework.integration.mongodb.outbound.MongoDbMessageHandler;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

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
	 * The test case that will test the null {@link MongoDbQueryFactory}
	 * instance
	 *
	 * @throws Exception
	 */
	@Test
	@MongoDbAvailable
	public void withNullMongoDbQueryFactory() throws Exception {
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "collection");
		try {
			adapter.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("MongoDbQueryFactory provided is null",
					e.getMessage());
		}
	}

	/**
	 * Test case that will test when the MongoDbQueryFactory returns a null
	 * static query
	 *
	 * @throws Exception
	 */
	@Test
	@MongoDbAvailable
	public void withNullStaticQuery() throws Exception {
		MongoDbQueryFactory factory = mock(MongoDbQueryFactory.class);
		when(factory.isStaticQuery()).thenReturn(true);
		when(factory.getQuery()).thenReturn(null);
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "collection");
		adapter.setQueryFactory(factory);
		try {
			adapter.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Provided Query instance is null",
					e.getMessage());
		}
	}

	/**
	 * The test case that will invoke the receive and tests that the getQuery is
	 * invoked only once on the factory
	 */
	@Test
	@MongoDbAvailable
	public void withMultipleReceiveForStaticQuery() throws Exception {
		MongoDbQueryFactory factory = mock(MongoDbQueryFactory.class);
		when(factory.isStaticQuery()).thenReturn(true);
		when(factory.getQuery()).thenReturn(new Query());
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "collection");
		adapter.setQueryFactory(factory);
		adapter.afterPropertiesSet();
		adapter.receive();
		verify(factory, times(1)).getQuery();
	}

	/**
	 * The test case that will invoke the receive and tests that the getQuery is
	 * invoked only the number of times receive is invoked
	 *
	 */
	@Test
	@MongoDbAvailable
	public void withMultipleReceiveForDynamicQuery() throws Exception {
		MongoDbQueryFactory factory = mock(MongoDbQueryFactory.class);
		when(factory.getQuery()).thenReturn(new Query());
		when(factory.isStaticQuery()).thenReturn(false);
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "collection");
		adapter.setQueryFactory(factory);
		adapter.afterPropertiesSet();
		adapter.receive();
		adapter.receive();
		adapter.receive();
		verify(factory, times(3)).getQuery();
	}

	@Test
	@MongoDbAvailable
	public void withNullDynamicQuery() throws Exception {
		MongoDbQueryFactory factory = mock(MongoDbQueryFactory.class);
		when(factory.isStaticQuery()).thenReturn(false);
		when(factory.getQuery()).thenReturn(null);
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "collection");
		adapter.setQueryFactory(factory);
		adapter.afterPropertiesSet();
		try {
			adapter.receive();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Provided Query instance is null",
					e.getMessage());
		}
	}

	@Test
	@MongoDbAvailable
	public void withFindAndUpdate() throws Exception {
		// first clear the Person collection
		setupPersonDocuments();
		final Query query = new Query().addCriteria(Criteria.where("read").exists(false));
		query.sort().on("age", Order.DESCENDING);
		MongoDbQueryFactory qFactory = mock(MongoDbQueryFactory.class);
		when(qFactory.getQuery()).thenReturn(query);
		when(qFactory.isStaticQuery()).thenReturn(true);
		when(qFactory.getUpdate()).thenReturn(new Update().set("read", "true"));
		when(qFactory.isUpdateStatic()).thenReturn(true);

		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "person");
		adapter.setQueryFactory(qFactory);
		adapter.setEntityClass(Person.class);
		adapter.afterPropertiesSet();
		String[] names = {"Person Three","Person Two","Person One"};
		for(int i = 0;i < 3;i++) {
			Message<Object> object = adapter.receive();
			Object payload = object.getPayload();
			Assert.assertNotNull("Expecting a non null person object",payload);
			Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
			Assert.assertEquals(names[i],((Person)payload).getName());
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	public void withFindOnly() throws Exception {
		setupPersonDocuments();
		MongoDbQueryFactory qFactory = mock(MongoDbQueryFactory.class);
		when(qFactory.getQuery()).thenReturn(
				new Query());
		when(qFactory.isStaticQuery()).thenReturn(true);
		MongoDbPollingChannelAdapter adapter = new MongoDbPollingChannelAdapter(
				prepareMongoFactory(), "person");
		adapter.setQueryFactory(qFactory);
		adapter.setEntityClass(Person.class);
		adapter.afterPropertiesSet();
		Message<Object> object = adapter.receive();
		Object payload = object.getPayload();
		Assert.assertNotNull("Expecting a non null List of Person object",payload);
		Assert.assertTrue(Person.class.isAssignableFrom(payload.getClass()));
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

		//now receive the two messages
		Query select = new Query();
		select.sort().on(MongoDbIntegrationConstants.CREATED_DATE, Order.ASCENDING);

		MongoDbQueryFactory qFactory = mock(MongoDbQueryFactory.class);
		when(qFactory.getQuery()).thenReturn(select);
		when(qFactory.isStaticQuery()).thenReturn(true);

		MongoDbPollingChannelAdapter inbound = new MongoDbPollingChannelAdapter(factory, collection);
		inbound.setQueryFactory(qFactory);
		inbound.afterPropertiesSet();

		Message<Object> object = inbound.receive();
		Assert.assertNotNull(object);
		Object payload = object.getPayload();
		Assert.assertTrue(String.class.isAssignableFrom(payload.getClass()));
		Assert.assertEquals("String One", (String)payload);


	}

}

class Person implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Id
	private ObjectId id;

	private String name;

	private Integer age;

	/**
	 * @param id
	 * @param name
	 * @param age
	 */
	public Person(String name, Integer age) {
		super();
		this.name = name;
		this.age = age;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
}

