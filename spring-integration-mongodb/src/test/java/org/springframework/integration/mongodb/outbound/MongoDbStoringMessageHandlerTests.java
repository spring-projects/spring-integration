/*
 * Copyright 2007-2023 the original author or authors.
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
import org.junit.Before;
import org.junit.Test;
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
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
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
 *
 * @since 2.2
 */
public class MongoDbStoringMessageHandlerTests extends MongoDbAvailableTests {

	private MongoTemplate template;

	private MongoDatabaseFactory mongoDbFactory;

	@Before
	public void setUp() {
		mongoDbFactory = prepareMongoFactory("foo");
		template = new MongoTemplate(mongoDbFactory);
	}


	@Test
	@MongoDbAvailable
	public void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbStoringMessageHandler((MongoDatabaseFactory) null));
	}

	@Test
	@MongoDbAvailable
	public void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbStoringMessageHandler((MongoOperations) null));
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithDefaultCollection() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "data");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithNamedCollection() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithMongoConverter() {

		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(this.mongoDbFactory);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		MappingMongoConverter converter = new TestMongoConverter(mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		handler.setMongoConverter(converter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
		verify(converter, times(1)).write(Mockito.any(), Mockito.any(Bson.class));
	}

	@Test
	@MongoDbAvailable
	public void validateMessageHandlingWithMongoTemplate() {
		MappingMongoConverter converter = new TestMongoConverter(this.mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		MongoTemplate writingTemplate = new MongoTemplate(this.mongoDbFactory, converter);


		MongoDbStoringMessageHandler handler = new MongoDbStoringMessageHandler(writingTemplate);
		handler.setCollectionNameExpression(new LiteralExpression("foo"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob")).build();
		handler.handleMessage(message);

		Query query = new BasicQuery("{'name' : 'Bob'}");
		Person person = template.findOne(query, Person.class, "foo");

		assertThat(person.getName()).isEqualTo("Bob");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
		verify(converter, times(1)).write(Mockito.any(), Mockito.any(Bson.class));
	}

}
