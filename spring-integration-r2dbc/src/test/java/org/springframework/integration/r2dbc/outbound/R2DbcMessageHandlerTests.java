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


import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 *  @author Rohan Mukesh
 *
 *  @since 5.4
 */
@SpringJUnitConfig
public class R2DbcMessageHandlerTests {

	@Autowired
	DatabaseClient client;

	@Autowired
	H2ConnectionFactory factory;

	R2dbcEntityTemplate entityTemplate;

	@Autowired
	PersonRepository personRepository;

	@Configuration
	@EnableR2dbcRepositories(considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(classes = PersonRepository.class, type = FilterType.ASSIGNABLE_TYPE))
	static class IntegrationTestConfiguration extends AbstractR2dbcConfiguration {

		@Bean
		@Override
		public ConnectionFactory connectionFactory() {
			return createConnectionFactory();
		}

	}

	public static ConnectionFactory createConnectionFactory() {
		return new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("r2dbc")
				.username("sa")
				.password("")
				.option("DB_CLOSE_DELAY=-1").build());
	}

	@BeforeEach
	public void setup() {
		entityTemplate = new R2dbcEntityTemplate(client);
		Hooks.onOperatorDebug();

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
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		personRepository.findAll()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	public void validateMessageHandlingWithInsertQueryCollection() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));
		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		handler.afterPropertiesSet();
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
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
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
				.consumeNextWith(p -> Assert.assertEquals(Optional.of(40), Optional.ofNullable(p.age)))
				.verifyComplete();
	}

	@Test
	public void validateMessageHandlingWithUpdateQueryCollection() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));


		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		handler.afterPropertiesSet();
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
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
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
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));

		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		handler.afterPropertiesSet();
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
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.setValuesExpression(new FunctionExpression<Message<?>>(Message::getPayload));

		handler.setQueryType(R2dbcMessageHandler.Type.INSERT);
		handler.setTableName("person");
		handler.afterPropertiesSet();
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

	interface PersonRepository extends ReactiveCrudRepository<Person, Integer> {

	}

}


