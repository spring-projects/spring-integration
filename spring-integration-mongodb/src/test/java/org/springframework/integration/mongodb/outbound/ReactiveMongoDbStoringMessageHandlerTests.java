/*
 * Copyright 2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import reactor.core.publisher.Mono;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author David Turanski
 *
 * @since 5.3
 */
public class ReactiveMongoDbStoringMessageHandlerTests extends MongoDbAvailableTests {

	private ReactiveMongoTemplate template;

	private ReactiveMongoDatabaseFactory mongoDbFactory;

	@Before
	public void setUp() {
		mongoDbFactory = this.prepareReactiveMongoFactory("foo");
		template = new ReactiveMongoTemplate(mongoDbFactory);
	}

	@Test
	@MongoDbAvailable
	public void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbStoringMessageHandler((ReactiveMongoDatabaseFactory) null));
	}

	@Test
	@MongoDbAvailable
	public void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbStoringMessageHandler((ReactiveMongoOperations) null));
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithDefaultCollection() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "data"));
		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");

	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithNamedCollection() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");

	}

	@Test
	@MongoDbAvailable
	public void errorOnMessageHandlingWithNullValuedExpression() {

		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setCollectionNameExpression(new LiteralExpression(null));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<Person> message = MessageBuilder.withPayload(createPerson("Bob")).build();
		AtomicBoolean errorOccurred = new AtomicBoolean();
		handler.handleMessage(message)
				.doOnError(e -> errorOccurred.set(true))
				.subscribe(aVoid -> assertThat(errorOccurred.get()).isTrue());
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithMongoConverter() {
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		MappingMongoConverter converter =
				new ReactiveTestMongoConverter(this.mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		handler.setMongoConverter(converter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithMongoTemplate() {
		MappingMongoConverter converter =
				new ReactiveTestMongoConverter(this.mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		ReactiveMongoTemplate writingTemplate = new ReactiveMongoTemplate(this.mongoDbFactory, converter);
		ReactiveMongoDbStoringMessageHandler handler = new ReactiveMongoDbStoringMessageHandler(writingTemplate);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		waitFor(handler.handleMessage(message));

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = waitFor(this.template.findOne(query, Person.class, "foo"));

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(3));
	}

}
