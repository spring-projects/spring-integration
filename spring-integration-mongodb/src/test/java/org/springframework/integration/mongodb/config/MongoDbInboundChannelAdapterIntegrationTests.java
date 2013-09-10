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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
/**
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class MongoDbInboundChannelAdapterIntegrationTests extends MongoDbAvailableTests {

	@Test
	@MongoDbAvailable
	public void testWithDefaultMongoFactory() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "data");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapter", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		assertNotNull(replyChannel.receive(1000));
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedMongoFactory() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "data");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapterNamedFactory", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<List<DBObject>> message = (Message<List<DBObject>>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).get("name"));
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoTemplate() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "data");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapterWithTemplate", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<Person> message = (Message<Person>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().getName());
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollection() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "foo");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapterWithNamedCollection", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollectionExpression() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "foo");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapterWithNamedCollectionExpression", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithOnSuccessDisposition() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "data");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("inboundAdapterWithOnSuccessDisposition", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		assertNotNull(replyChannel.receive(1000));
		Thread.sleep(300);
		assertNull(replyChannel.receive(1000));
		spca.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoConverter() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Bob"), "data");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("inbound-adapter-config.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("mongoInboundAdapterWithConverter", SourcePollingChannelAdapter.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		spca.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(1000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		assertNotNull(replyChannel.receive(1000));
		spca.stop();
	}

	@Test(expected=BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithQueryAndQueryExpression() throws Exception{
		new ClassPathXmlApplicationContext("inbound-fail-q-qex.xml", this.getClass());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithFactoryAndTemplate() throws Exception{
		new ClassPathXmlApplicationContext("inbound-fail-factory-template.xml", this.getClass());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithCollectionAndCollectioinExpression() throws Exception{
		new ClassPathXmlApplicationContext("inbound-fail-c-cex.xml", this.getClass());
	}

	@Test(expected=BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithTemplateAndConverter() throws Exception{
		new ClassPathXmlApplicationContext("inbound-fail-converter-template.xml", this.getClass());
	}

	public static class DocumentCleaner {
		public void remove(MongoOperations mongoOperations, Object target, String collectionName) {
			if (target instanceof List<?>){
				List<?> documents = (List<?>) target;
				for (Object document : documents) {
					mongoOperations.remove(new BasicQuery(JSON.serialize(document)), collectionName);
				}
			}
		}
	}

}
