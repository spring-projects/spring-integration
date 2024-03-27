/*
 * Copyright 2007-2024 the original author or authors.
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

import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 2.2
 */
class MongoDbStoringMessageHandlerTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	private MongoTemplate template;

	@BeforeEach
	public void setUp() {
		template = new MongoTemplate(MONGO_DATABASE_FACTORY);
	}

	@Test
	void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbStoringMessageHandler((MongoDatabaseFactory) null));
	}

	@Test
	void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbStoringMessageHandler((MongoOperations) null));
	}

	@Test
	void validateMessageHandlingWithDefaultCollection() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(MONGO_DATABASE_FACTORY);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "data");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateMessageHandlingWithNamedCollection() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(MONGO_DATABASE_FACTORY);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateMessageHandlingWithMongoConverter() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(MONGO_DATABASE_FACTORY);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		MappingMongoConverter converter = new TestMongoConverter(MONGO_DATABASE_FACTORY, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		handler.setMongoConverter(converter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
		verify(converter, times(1)).write(Mockito.any(), Mockito.any(Bson.class));
	}

	@Test
	void validateMessageHandlingWithMongoTemplate() {
		MappingMongoConverter converter = new TestMongoConverter(MONGO_DATABASE_FACTORY, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		MongoTemplate writingTemplate = new MongoTemplate(MONGO_DATABASE_FACTORY, converter);

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(writingTemplate);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
		verify(converter, times(1)).write(Mockito.any(), Mockito.any(Bson.class));
	}

}
