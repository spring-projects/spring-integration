/*
 * Copyright 2019-2022 the original author or authors.
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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author David Turanski
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.3
 */
@SpringJUnitConfig
@DirtiesContext
class ReactiveMongoDbStoringMessageHandlerTests implements MongoDbContainerTest {

	public static ReactiveMongoDatabaseFactory REACTIVE_MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		REACTIVE_MONGO_DATABASE_FACTORY = MongoDbContainerTest.createReactiveMongoDbFactory();
	}

	private ReactiveMongoTemplate template;

	@Autowired
	private MessageChannel input;

	@BeforeEach
	public void setUp() {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY, "foo");
		this.template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);
	}

	@Test
	void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbStoringMessageHandler((ReactiveMongoDatabaseFactory) null));
	}

	@Test
	void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbStoringMessageHandler((ReactiveMongoOperations) null));
	}

	@Test
	void validateMessageHandlingWithDefaultCollection() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(REACTIVE_MONGO_DATABASE_FACTORY);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "data"));
		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");

	}

	@Test
	void validateMessageHandlingWithNamedCollection() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(REACTIVE_MONGO_DATABASE_FACTORY);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();

		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");

	}

	@Test
	void errorOnMessageHandlingWithNullValuedExpression() {

		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(REACTIVE_MONGO_DATABASE_FACTORY);
		handler.setCollectionNameExpression(new LiteralExpression(null));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();

		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		AtomicBoolean errorOccurred = new AtomicBoolean();
		handler.handleMessage(message)
				.doOnError(e -> errorOccurred.set(true))
				.subscribe(aVoid -> assertThat(errorOccurred.get()).isTrue());
	}

	@Test
	void validateMessageHandlingWithMongoConverter() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(REACTIVE_MONGO_DATABASE_FACTORY);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		MappingMongoConverter converter =
				new ReactiveTestMongoConverter(REACTIVE_MONGO_DATABASE_FACTORY, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		handler.setMongoConverter(converter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateMessageHandlingWithMongoTemplate() {
		MappingMongoConverter converter =
				new ReactiveTestMongoConverter(REACTIVE_MONGO_DATABASE_FACTORY, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		ReactiveMongoTemplate writingTemplate = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY, converter);
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(writingTemplate);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void testReactiveMongoMessageHandlerFromApplicationContext() {
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		this.input.send(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");

		await().untilAsserted(() ->
				assertThat(waitFor(this.template.findOne(query, Person.class, "data")))
						.isNotNull()
						.extracting("name", "address.state").contains("Bob", "PA"));
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(10));
	}

}
