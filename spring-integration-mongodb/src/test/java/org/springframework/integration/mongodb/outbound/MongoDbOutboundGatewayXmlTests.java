/*
 * Copyright 2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Xavier Padró
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class MongoDbOutboundGatewayXmlTests extends MongoDbAvailableTests {

	private static final String COLLECTION_NAME = "data";

	@Autowired
	private ApplicationContext context;

	@Before
	public void setUp() throws Exception {
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory);

		mongoTemplate.save(this.createPerson("Artem"), COLLECTION_NAME);
		mongoTemplate.save(this.createPerson("Gary"), COLLECTION_NAME);
		mongoTemplate.save(this.createPerson("Oleg"), COLLECTION_NAME);
		mongoTemplate.save(this.createPerson("Xavi"), COLLECTION_NAME);
	}

	@After
	public void cleanUp() throws Exception {
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory);

		mongoTemplate.dropCollection(COLLECTION_NAME);
	}


	@Test
	@MongoDbAvailable
	public void testSingleQuery() throws Exception {
		EventDrivenConsumer consumer = context.getBean("gatewaySingleQuery", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder.withPayload("").build();
		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		Person person = getPerson(result);
		assertEquals("Xavi", person.getName());
	}

	@Test
	@MongoDbAvailable
	public void testSingleQueryWithTemplate() throws Exception {
		EventDrivenConsumer consumer = context.getBean("gatewayWithTemplate", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder.withPayload("").build();
		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		Person person = getPerson(result);
		assertEquals("Xavi", person.getName());
	}

	@Test
	@MongoDbAvailable
	public void testSingleQueryExpression() throws Exception {
		EventDrivenConsumer consumer = context.getBean("gatewaySingleQueryExpression", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder
				.withPayload("")
				.setHeader("query", "{'name' : 'Gary'}")
				.setHeader("collectionName", "data")
				.build();

		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		Person person = getPerson(result);
		assertEquals("Gary", person.getName());
	}

	@Test
	@MongoDbAvailable
	public void testQueryExpression() throws Exception {
		EventDrivenConsumer consumer = context.getBean("gatewayQueryExpression", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder
				.withPayload("")
				.setHeader("query", "{}")
				.setHeader("collectionName", "data")
				.build();

		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		List<Person> persons = getPersons(result);
		assertEquals(4, persons.size());
	}

	@Test
	@MongoDbAvailable
	public void testQueryExpressionWithLimit() throws Exception {
		EventDrivenConsumer consumer = context.getBean("gatewayQueryExpressionLimit", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder
				.withPayload("")
				.setHeader("collectionName", "data")
				.build();

		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		List<Person> persons = getPersons(result);
		assertEquals(2, persons.size());
	}

	private Person getPerson(Message<?> message) {
		return (Person) message.getPayload();
	}

	@SuppressWarnings("unchecked")
	private List<Person> getPersons(Message<?> message) {
		return (List<Person>) message.getPayload();
	}

}
