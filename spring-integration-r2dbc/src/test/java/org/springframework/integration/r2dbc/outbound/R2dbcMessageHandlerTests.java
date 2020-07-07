/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.r2dbc.outbound;


import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.r2dbc.config.R2dbcIntegrationTestConfiguration;
import org.springframework.integration.r2dbc.entity.Person;
import org.springframework.integration.r2dbc.repository.PersonRepository;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.r2dbc.h2.H2ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Rohan Mukesh
 *
 * @since 5.4
 */
@SpringJUnitConfig(R2dbcIntegrationTestConfiguration.class)
public class R2dbcMessageHandlerTests {

	@Autowired
	DatabaseClient client;

	@Autowired
	H2ConnectionFactory factory;

	@Autowired
	PersonRepository personRepository;

	@Autowired
	R2dbcMessageHandler handler;

	@BeforeEach
	public void setup() {
		Hooks.onOperatorDebug();
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableNameExpression(null);
		List<String> statements = Arrays.asList(
				"DROP TABLE IF EXISTS person;",
				"CREATE table person (id INT AUTO_INCREMENT NOT NULL, name VARCHAR2, age INT NOT NULL);");

		statements.forEach(it -> client.execute(it)
				.fetch()
				.rowsUpdated()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete());

	}

	@Test
	public void validateMessageHandlingWithDefaultInsertCollection() {
		Message<Person> message = MessageBuilder.withPayload(createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		personRepository.findAll()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	public void validateMessageHandlingWithInsertQueryCollection() {
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		Map<String, Object> payload = new HashMap<>();
		payload.put("name", "rohan");
		payload.put("age", 35);
		Message<?> message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		Flux<?> all = client.execute("SELECT name, age FROM person")
				.fetch().all();
		all.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

	}

	@Test
	public void validateMessageHandlingWithDefaultUpdateCollection() {
		Message<Person> message = MessageBuilder.withPayload(createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		handler.setQueryType(R2dbcMessageHandler.Type.UPDATE);

		Person person = this.client.select()
				.from("person")
				.as(Person.class)
				.fetch()
				.first()
				.block();

		person.setAge(40);

		message = MessageBuilder.withPayload(person)
				.build();
		waitFor(handler.handleMessage(message));

		personRepository.findAll()
				.as(StepVerifier::create)
				.consumeNextWith(p -> Assert.assertEquals(Optional.of(40), Optional.ofNullable(p.getAge())))
				.verifyComplete();
	}

	@Test
	public void validateMessageHandlingWithUpdateQueryCollection() {
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		Map<String, Object> payload = new HashMap<>();
		payload.put("name", "Bob");
		payload.put("age", 35);
		Message<?> message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		payload = new HashMap<>();
		payload.put("name", "Rob");
		payload.put("age", 43);
		message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		payload = new HashMap<>();
		handler.setQueryType(R2dbcMessageHandler.Type.UPDATE);

		Object insertedId = client.execute("SELECT id FROM person")
				.fetch()
				.first()
				.block()
				.get("id");

		handler.setCriteriaExpression(new FunctionExpression<Message<?>>((m) -> Criteria.where("id").is(insertedId)));
		payload.put("age", 40);

		message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		Flux<?> all = client.execute("SELECT age,name FROM person where age=40")
				.fetch().all();
		all.as(StepVerifier::create)
				.consumeNextWith(response -> Assert.assertEquals("{AGE=40, NAME=Bob}", response.toString()))
				.verifyComplete();

	}

	@Test
	public void validateMessageHandlingWithDefaultDeleteCollection() {
		Message<Person> message = MessageBuilder.withPayload(createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		Person person = this.client
				.select()
				.from("person")
				.as(Person.class)
				.fetch()
				.first()
				.block();

		handler.setQueryType(R2dbcMessageHandler.Type.DELETE);
		message = MessageBuilder.withPayload(person).build();
		waitFor(handler.handleMessage(message));

		personRepository.findAll()
				.as(StepVerifier::create)
				.expectNextCount(0)
				.verifyComplete();
	}

	@Test
	public void validateMessageHandlingWithDeleteQueryCollection() {
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		Map<String, Object> payload = new HashMap<>();
		payload.put("name", "Bob");
		payload.put("age", 35);
		Message<?> message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		payload = new HashMap<>();
		handler.setQueryType(R2dbcMessageHandler.Type.DELETE);

		Object insertedId = client.execute("SELECT id FROM person")
				.fetch()
				.first()
				.block()
				.get("id");

		handler.setCriteriaExpression(new FunctionExpression<Message<?>>((m) -> Criteria.where("id").is(insertedId)));
		message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		Flux<?> all = client.execute("SELECT age,name FROM person where age=35")
				.fetch().all();
		all.as(StepVerifier::create)
				.expectNextCount(0)
				.verifyComplete();

	}

	@Test
	public void validateMessageHandlingWithDeleteQueryCollection_MultipleRows() {
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		Map<String, Object> payload = new HashMap<>();
		payload.put("name", "Bob");
		payload.put("age", 35);
		Message<?> message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		payload = new HashMap<>();
		payload.put("name", "Rob");
		payload.put("age", 40);
		message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		payload = new HashMap<>();
		handler.setQueryType(R2dbcMessageHandler.Type.DELETE);

		Object insertedId = client.execute("SELECT id FROM person")
				.fetch()
				.first()
				.block()
				.get("id");

		handler.setCriteriaExpression(new FunctionExpression<Message<?>>((m) -> Criteria.where("id").is(insertedId)));
		message = MessageBuilder.withPayload(payload).build();
		waitFor(handler.handleMessage(message));

		Flux<?> all = client.execute("SELECT age,name FROM person where age=40")
				.fetch().all();
		all.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

	}


	private Person createPerson(String bob, Integer age) {
		return new Person(bob, age);
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(10));
	}

}


