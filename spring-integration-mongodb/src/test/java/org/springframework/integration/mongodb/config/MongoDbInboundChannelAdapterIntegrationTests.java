/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.mongodb.config;

import java.util.List;

import com.mongodb.BasicDBObject;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.aop.ReceiveMessageAdvice;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Yaron Yamin
 * @author Artem Vozhdayenko
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
class MongoDbInboundChannelAdapterIntegrationTests implements MongoDbContainerTest {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private QueueChannel replyChannel;

	@Autowired
	private QueueChannel afterCommitChannel;

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
	void testWithDefaultMongoFactory() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "data");

		this.mongoInboundAdapter.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");
		assertThat(this.replyChannel.receive(10000)).isNotNull();

		this.mongoInboundAdapter.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithNamedMongoFactory() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "data");

		this.mongoInboundAdapterNamedFactory.start();

		@SuppressWarnings("unchecked")
		Message<List<BasicDBObject>> message = (Message<List<BasicDBObject>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0)).containsEntry("name", "Bob");

		this.mongoInboundAdapterNamedFactory.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithMongoTemplate() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "data");

		this.mongoInboundAdapterWithTemplate.start();

		@SuppressWarnings("unchecked")
		Message<Person> message = (Message<Person>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().getName()).isEqualTo("Bob");

		this.mongoInboundAdapterWithTemplate.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithNamedCollection() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "foo");

		this.mongoInboundAdapterWithNamedCollection.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");

		this.mongoInboundAdapterWithNamedCollection.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithQueryExpression() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "foo");
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "foo");
		this.mongoInboundAdapterWithQueryExpression.start();
		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).hasSize(1);
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");
		this.mongoInboundAdapterWithQueryExpression.stop();
	}

	@Test
	void testWithStringQueryExpression() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "foo");
		this.mongoInboundAdapterWithStringQueryExpression.start();
		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");
		this.mongoInboundAdapterWithStringQueryExpression.stop();
	}

	@Test
	void testWithNamedCollectionExpression() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "foo");

		this.mongoInboundAdapterWithNamedCollectionExpression.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");

		this.mongoInboundAdapterWithNamedCollectionExpression.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithOnSuccessDisposition() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "data");

		this.inboundAdapterWithOnSuccessDisposition.start();

		assertThat(replyChannel.receive(10000)).isNotNull();
		assertThat(replyChannel.receive(100)).isNull();

		assertThat(this.afterCommitChannel.receive(10000)).isNotNull();

		assertThat(this.mongoTemplate.findOne(new Query(Criteria.where("name").is("Bob")), Person.class, "data"))
				.isNull();
		this.inboundAdapterWithOnSuccessDisposition.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testWithMongoConverter() {
		this.mongoTemplate.save(MongoDbContainerTest.createPerson("Bob"), "data");

		this.mongoInboundAdapterWithConverter.start();

		@SuppressWarnings("unchecked")
		Message<List<Person>> message = (Message<List<Person>>) replyChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().get(0).getName()).isEqualTo("Bob");
		assertThat(replyChannel.receive(10000)).isNotNull();

		this.mongoInboundAdapterWithConverter.stop();
		this.replyChannel.purge(null);
	}

	@Test
	void testFailureWithQueryAndQueryExpression() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-fail-q-qex.xml", MongoDbInboundChannelAdapterIntegrationTests.class))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

	@Test
	void testFailureWithFactoryAndTemplate() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-fail-factory-template.xml", MongoDbInboundChannelAdapterIntegrationTests.class))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

	@Test
	void testFailureWithCollectionAndCollectionExpression() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-fail-c-cex.xml", MongoDbInboundChannelAdapterIntegrationTests.class))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

	@Test
	void testFailureWithTemplateAndConverter() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("inbound-fail-converter-template.xml", MongoDbInboundChannelAdapterIntegrationTests.class))
				.isInstanceOf(BeanDefinitionParsingException.class);
	}

	public static class DocumentCleaner {

		public void remove(MongoOperations mongoOperations, Object target, String collectionName) {
			if (target instanceof List<?>) {
				List<?> documents = (List<?>) target;
				for (Object document : documents) {
					mongoOperations.remove(document, collectionName);
				}
			}
		}

	}

	public static final class TestMessageSourceAdvice implements ReceiveMessageAdvice {

		@Override
		public Message<?> afterReceive(Message<?> result, Object source) {
			return result;
		}

	}

}
