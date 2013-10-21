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
package org.springframework.integration.mongodb.config;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
/**
 * @author Oleg Zhurakousky
 */
public class MongoDbOutboundChannelAdapterIntegrationTests extends MongoDbAvailableTests {

	@Test
	@MongoDbAvailable
	public void testWithDefaultMongoFactory() throws Exception{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbAvailableTests.Person>(this.createPerson("Bob"));
		channel.send(message);

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data"));
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollection() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithNamedCollection", MessageChannel.class);
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).setHeader("collectionName", "foo").build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo"));
	}

	@Test
	@MongoDbAvailable
	public void testWithTemplate() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).setHeader("collectionName", "foo").build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo"));
	}

	@Test
	@MongoDbAvailable
	public void testSavingDbObject() throws Exception{

		BasicDBObject dbObject = (BasicDBObject) JSON.parse("{'foo' : 'bar'}");

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);
		Message<BasicDBObject> message = MessageBuilder.withPayload(dbObject).setHeader("collectionName", "foo").build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo"));
	}

	@Test
	@MongoDbAvailable
	public void testSavingJSONString() throws Exception{

		String object = "{'foo' : 'bar'}";

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);
		Message<String> message = MessageBuilder.withPayload(object).setHeader("collectionName", "foo").build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo"));
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoConverter() throws Exception{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithConverter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbAvailableTests.Person>(this.createPerson("Bob"));
		channel.send(message);

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertNotNull(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data"));
	}
}
