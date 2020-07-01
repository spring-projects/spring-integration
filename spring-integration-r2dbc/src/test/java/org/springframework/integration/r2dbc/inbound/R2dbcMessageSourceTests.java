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

package org.springframework.integration.r2dbc.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.r2dbc.BadSqlGrammarException;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.r2dbc.config.R2dbcIntegrationTestConfiguration;
import org.springframework.integration.r2dbc.entity.Person;
import org.springframework.integration.r2dbc.outbound.R2dbcMessageHandler;
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
 *  @author Rohan Mukesh
 *
 *  @since 5.4
 */
@SpringJUnitConfig (R2dbcIntegrationTestConfiguration.class)
public class R2dbcMessageSourceTests {

	@Autowired
	DatabaseClient client;

	@Autowired
	H2ConnectionFactory factory;

	R2dbcEntityTemplate entityTemplate;

	@Autowired
	PersonRepository personRepository;

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
	public void validateSuccessfulQueryWithSingleElementFluxOfQueryObject() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> Query.query(Criteria.where("id").is(1))));
		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Bob"))
				.verifyComplete();
	}

	@Test
	public void validateSuccessfulQueryWithMultipleElementFluxOfQueryObject() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> Query.empty()));
		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Bob"))
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Tom"))
				.verifyComplete();
	}

	@Test
	public void validateSuccessfulQueryWithSingleElementFluxOfStringObject() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> "select * from Person Where id=1"));
		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Bob"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulQueryWithMultipleElementFluxOfStringObject() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> "select * from Person"));
		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Bob"))
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Tom"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulQueryWithOneExpectedElement() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> "select * from Person where id=1"));
		StepVerifier.create((Mono<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Bob"))
				.verifyComplete();

	}

	@Test
	public void validateExceptionQueryWithOneExpectedElement() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setQueryExpression(new FunctionExpression<>((m) -> "select * from Person"));
		StepVerifier.create((Mono<Person>) messageSource.receive().getPayload())
				.expectErrorMatches(throwable -> throwable instanceof IncorrectResultSizeDataAccessException)
				.verify();
	}

	@Test
	public void testBadQueryExpression() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setQueryExpression(new ValueExpression("Incorrect"));
		StepVerifier.create((Mono<Person>) messageSource.receive().getPayload())
				.expectErrorMatches(throwable -> throwable instanceof BadSqlGrammarException)
				.verify();
	}

	@Test
	public void testAnyOtherObjectQueryExpression() {
		R2dbcMessageHandler handler = new R2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		message = MessageBuilder.withPayload(this.createPerson("Tom", 40)).build();
		waitFor(handler.handleMessage(message));

		R2dbcMessageSource messageSource = new R2dbcMessageSource(this.entityTemplate);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setQueryExpression(new ValueExpression(new Object()));

		Assertions.assertThrows(IllegalStateException.class, () -> messageSource.receive().getPayload());
	}

	private Person createPerson(String bob, Integer age) {
		return new Person(bob, age);
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(10));
	}

}
