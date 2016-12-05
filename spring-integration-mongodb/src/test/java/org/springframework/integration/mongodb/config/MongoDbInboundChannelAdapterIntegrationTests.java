/*
 * Copyright 2002-2016 the original author or authors.
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
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.aop.AbstractMessageSourceAdvice;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Yaron Yamin
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MongoDbInboundChannelAdapterIntegrationTests extends MongoDbAvailableTests {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	@Qualifier("mongoInboundAdapter")
	private SourcePollingChannelAdapter mongoInboundAdapter;

	@Autowired
	@Qualifier("mongoInboundAdapterNamedFactory")
	private SourcePollingChannelAdapter mongoInboundAdapterNamedFactory;

	@Autowired
	@Qualifier("mongoInboundAdapterWithTemplate")
	private SourcePollingChannelAdapter mongoInboundAdapterWithTemplate;

	@Autowired
	@Qualifier("mongoInboundAdapterWithNamedCollection")
	private SourcePollingChannelAdapter mongoInboundAdapterWithNamedCollection;

	@Autowired
	@Qualifier("mongoInboundAdapterWithQueryExpression")
	private SourcePollingChannelAdapter mongoInboundAdapterWithQueryExpression;

	@Autowired
	@Qualifier("mongoInboundAdapterWithStringQueryExpression")
	private SourcePollingChannelAdapter mongoInboundAdapterWithStringQueryExpression;

	@Autowired
	@Qualifier("mongoInboundAdapterWithNamedCollectionExpression")
	private SourcePollingChannelAdapter mongoInboundAdapterWithNamedCollectionExpression;

	@Autowired
	@Qualifier("inboundAdapterWithOnSuccessDisposition")
	private SourcePollingChannelAdapter inboundAdapterWithOnSuccessDisposition;

	@Autowired
	@Qualifier("mongoInboundAdapterWithConverter")
	private SourcePollingChannelAdapter mongoInboundAdapterWithConverter;

	@Test
	@MongoDbAvailable
	public void testWithDefaultMongoFactory() throws Exception {
		this.mongoTemplate.save(createPerson("Bob"), "data");

		this.mongoInboundAdapter.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		assertNotNull(this.replyChannel.receive(10000));

		this.mongoInboundAdapter.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedMongoFactory() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "data");

		this.mongoInboundAdapterNamedFactory.start();

		@SuppressWarnings("unchecked")
		Message<List<DBObject>> message = (Message<List<DBObject>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).get("name"));

		this.mongoInboundAdapterNamedFactory.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoTemplate() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "data");

		this.mongoInboundAdapterWithTemplate.start();

		@SuppressWarnings("unchecked")
		Message<Person> message = (Message<Person>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().getName());

		this.mongoInboundAdapterWithTemplate.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollection() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "foo");

		this.mongoInboundAdapterWithNamedCollection.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());

		this.mongoInboundAdapterWithNamedCollection.stop();
	}
	@Test
	@MongoDbAvailable
	public void testWithQueryExpression() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "foo");
		this.mongoTemplate.save(this.createPerson("Bob"), "foo");
		this.mongoInboundAdapterWithQueryExpression.start();
		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals(1, message.getPayload().size());
		assertEquals("Bob", message.getPayload().get(0).getName());
		this.mongoInboundAdapterWithQueryExpression.stop();
	}
	@Test
	@MongoDbAvailable
	public void testWithStringQueryExpression() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "foo");
		this.mongoInboundAdapterWithStringQueryExpression.start();
		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		this.mongoInboundAdapterWithStringQueryExpression.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollectionExpression() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "foo");

		this.mongoInboundAdapterWithNamedCollectionExpression.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());

		this.mongoInboundAdapterWithNamedCollectionExpression.stop();
	}

	@Test
	@MongoDbAvailable
	public void testWithOnSuccessDisposition() throws Exception {
		this.mongoTemplate.save(createPerson("Bob"), "data");

		this.inboundAdapterWithOnSuccessDisposition.start();

		assertNotNull(replyChannel.receive(10000));
		assertNull(replyChannel.receive(100));

		this.inboundAdapterWithOnSuccessDisposition.stop();

		assertNull(this.mongoTemplate.findOne(new Query(Criteria.where("name").is("Bob")), Person.class, "data"));
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoConverter() throws Exception {
		this.mongoTemplate.save(this.createPerson("Bob"), "data");

		this.mongoInboundAdapterWithConverter.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Bob", message.getPayload().get(0).getName());
		assertNotNull(replyChannel.receive(10000));

		this.mongoInboundAdapterWithConverter.stop();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithQueryAndQueryExpression() throws Exception {
		new ClassPathXmlApplicationContext("inbound-fail-q-qex.xml", this.getClass()).close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithFactoryAndTemplate() throws Exception {
		new ClassPathXmlApplicationContext("inbound-fail-factory-template.xml", this.getClass()).close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithCollectionAndCollectionExpression() throws Exception {
		new ClassPathXmlApplicationContext("inbound-fail-c-cex.xml", this.getClass()).close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	@MongoDbAvailable
	public void testFailureWithTemplateAndConverter() throws Exception {
		new ClassPathXmlApplicationContext("inbound-fail-converter-template.xml", this.getClass()).close();
	}

	public static class DocumentCleaner {

		public void remove(MongoOperations mongoOperations, Object target, String collectionName) {
			if (target instanceof List<?>) {
				List<?> documents = (List<?>) target;
				for (Object document : documents) {
					mongoOperations.remove(new BasicQuery(JSON.serialize(document)), collectionName);
				}
			}
		}

	}

	public static final class TestMessageSourceAdvice extends AbstractMessageSourceAdvice {

		@Override
		public boolean beforeReceive(MessageSource<?> source) {
			return true;
		}

		@Override
		public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
			return result;
		}

	}

}
