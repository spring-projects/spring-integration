/*
 * Copyright 2016-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mongodb.outbound;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Xavier Padro
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
class MongoDbOutboundGatewayXmlTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	private static final String COLLECTION_NAME = "data";

	@Autowired
	private ApplicationContext context;

	@BeforeEach
	public void setUp() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);
		MongoTemplate mongoTemplate = new MongoTemplate(MONGO_DATABASE_FACTORY);

		mongoTemplate.save(MongoDbContainerTest.createPerson("Artem"), COLLECTION_NAME);
		mongoTemplate.save(MongoDbContainerTest.createPerson("Gary"), COLLECTION_NAME);
		mongoTemplate.save(MongoDbContainerTest.createPerson("Oleg"), COLLECTION_NAME);
		mongoTemplate.save(MongoDbContainerTest.createPerson("Xavi"), COLLECTION_NAME);
	}

	@AfterEach
	public void cleanUp() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);
		MongoTemplate mongoTemplate = new MongoTemplate(MONGO_DATABASE_FACTORY);

		mongoTemplate.dropCollection(COLLECTION_NAME);
	}

	@Test
	void testSingleQuery() {
		EventDrivenConsumer consumer = context.getBean("gatewaySingleQuery", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder.withPayload("").build();
		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		Person person = getPerson(result);
		assertThat(person.getName()).isEqualTo("Xavi");
	}

	@Test
	void testSingleQueryWithTemplate() {
		EventDrivenConsumer consumer = context.getBean("gatewayWithTemplate", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder.withPayload("").build();
		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		Person person = getPerson(result);
		assertThat(person.getName()).isEqualTo("Xavi");
	}

	@Test
	void testSingleQueryExpression() {
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
		assertThat(person.getName()).isEqualTo("Gary");
	}

	@Test
	void testQueryExpression() {
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
		assertThat(persons).hasSize(4);
	}

	@Test
	void testQueryExpressionWithLimit() {
		EventDrivenConsumer consumer = context.getBean("gatewayQueryExpressionLimit", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder
				.withPayload("")
				.setHeader("collectionName", "data")
				.build();

		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		List<Person> persons = getPersons(result);
		assertThat(persons).hasSize(2);
	}

	@Test
	void testCollectionCallback() {
		EventDrivenConsumer consumer = context.getBean("gatewayCollectionCallback", EventDrivenConsumer.class);
		PollableChannel outChannel = context.getBean("out", PollableChannel.class);

		Message<String> message = MessageBuilder
				.withPayload("")
				.setHeader("collectionName", "data")
				.build();

		consumer.getHandler().handleMessage(message);

		Message<?> result = outChannel.receive(10000);
		long personsCount = (Long) result.getPayload();
		assertThat(personsCount).isEqualTo(4);
	}

	private Person getPerson(Message<?> message) {
		return (Person) message.getPayload();
	}

	@SuppressWarnings("unchecked")
	private List<Person> getPersons(Message<?> message) {
		return (List<Person>) message.getPayload();
	}

}
