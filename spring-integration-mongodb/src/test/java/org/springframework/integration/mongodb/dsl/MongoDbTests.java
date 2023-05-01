/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.mongodb.dsl;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.QueueChannelSpec;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.mongodb.outbound.MessageCollectionCallback;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * @author Xavier Padro
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
class MongoDbTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
		REACTIVE_MONGO_DATABASE_FACTORY = MongoDbContainerTest.createReactiveMongoDbFactory();
	}

	static ReactiveMongoDatabaseFactory REACTIVE_MONGO_DATABASE_FACTORY;

	private static final String COLLECTION_NAME = "data";

	@Autowired
	private PollableChannel getResultChannel;

	@Autowired
	@Qualifier("gatewaySingleQueryFlow.input")
	private MessageChannel gatewaySingleQueryFlow;

	@Autowired
	@Qualifier("gatewaySingleQueryWithTemplateFlow.input")
	private MessageChannel gatewaySingleQueryWithTemplateFlow;

	@Autowired
	@Qualifier("gatewaySingleQueryExpressionFlow.input")
	private MessageChannel gatewaySingleQueryExpressionFlow;

	@Autowired
	@Qualifier("gatewayQueryExpressionFlow.input")
	private MessageChannel gatewayQueryExpressionFlow;

	@Autowired
	@Qualifier("gatewayQueryExpressionLimitFlow.input")
	private MessageChannel gatewayQueryExpressionLimitFlow;

	@Autowired
	@Qualifier("gatewayQueryFunctionFlow.input")
	private MessageChannel gatewayQueryFunctionFlow;

	@Autowired
	@Qualifier("gatewayCollectionNameFunctionFlow.input")
	private MessageChannel gatewayCollectionNameFunctionFlow;

	@Autowired
	@Qualifier("gatewayCollectionCallbackFlow.input")
	private MessageChannel gatewayCollectionCallbackFlow;

	@Autowired
	private MongoOperations mongoTemplate;

	@BeforeEach
	public void setUp() {
		createPersons();
	}

	@AfterEach
	public void cleanUp() {
		mongoTemplate.dropCollection(COLLECTION_NAME);
	}

	@Test
	void testGatewayWithSingleQuery() {
		gatewaySingleQueryFlow.send(MessageBuilder
				.withPayload("Xavi")
				.setHeader("collection", "data")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		Person retrievedPerson = (Person) result.getPayload();
		assertThat(retrievedPerson.getName()).isEqualTo("Xavi");
	}

	@Test
	void testGatewayWithSingleQueryWithTemplate() {
		gatewaySingleQueryWithTemplateFlow.send(MessageBuilder.withPayload("Xavi").build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		Person retrievedPerson = (Person) result.getPayload();
		assertThat(retrievedPerson.getName()).isEqualTo("Xavi");
	}

	@Test
	void testGatewayWithSingleQueryExpression() {
		gatewaySingleQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{'name' : 'Artem'}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		Person retrievedPerson = (Person) result.getPayload();
		assertThat(retrievedPerson.getName()).isEqualTo("Artem");
	}

	@Test
	void testGatewayWithSingleQueryExpressionNoPersonFound() {
		assertThatThrownBy(() -> gatewaySingleQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{'name' : 'NonExisting'}")
				.build()))
				.isInstanceOf(ReplyRequiredException.class);
	}

	@Test
	void testGatewayWithQueryExpression() {
		gatewayQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		List<Person> retrievedPersons = getPersons(result);
		assertThat(retrievedPersons).hasSize(4);
	}

	@Test
	void testGatewayWithQueryExpressionAndLimit() {
		gatewayQueryExpressionLimitFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		List<Person> retrievedPersons = getPersons(result);
		assertThat(retrievedPersons).hasSize(2);
	}

	@Test
	void testGatewayWithQueryFunction() {
		gatewayQueryFunctionFlow.send(MessageBuilder
				.withPayload("Gary")
				.setHeader("collection", "data")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		Person person = (Person) result.getPayload();
		assertThat(person.getName()).isEqualTo("Gary");
	}

	@Test
	void testGatewayWithCollectionNameFunction() {
		gatewayCollectionNameFunctionFlow.send(MessageBuilder
				.withPayload("data")
				.setHeader("query", "{'name' : 'Gary'}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		Person person = (Person) result.getPayload();
		assertThat(person.getName()).isEqualTo("Gary");
	}

	@Test
	void testGatewayWithCollectionCallback() {
		gatewayCollectionCallbackFlow.send(MessageBuilder
				.withPayload("")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertThat(result).isNotNull();
		long count = (Long) result.getPayload();
		assertThat(count).isEqualTo(4);
	}

	@SuppressWarnings("unchecked")
	private List<Person> getPersons(Message<?> message) {
		return (List<Person>) message.getPayload();
	}

	private void createPersons() {
		BulkOperations bulkOperations = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, COLLECTION_NAME);
		bulkOperations.insert(Arrays.asList(
				MongoDbContainerTest.createPerson("Artem"),
				MongoDbContainerTest.createPerson("Gary"),
				MongoDbContainerTest.createPerson("Oleg"),
				MongoDbContainerTest.createPerson("Xavi")));
		bulkOperations.execute();
	}

	@Autowired
	@Qualifier("reactiveStore.input")
	private MessageChannel reactiveStoreInput;

	@Test
	void testReactiveMongoDbMessageHandler() {
		this.reactiveStoreInput.send(MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob")).build());

		ReactiveMongoTemplate reactiveMongoTemplate = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);

		await().untilAsserted(() ->
				assertThat(
						reactiveMongoTemplate.findOne(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data")
								.block(Duration.ofSeconds(10)))
						.isNotNull()
						.extracting("name", "address.state").contains("Bob", "PA"));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow gatewaySingleQueryFlow() {
			return f -> f
					.handle(queryOutboundGateway("{name: 'Xavi'}", true))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewaySingleQueryWithTemplateFlow() {
			return f -> f
					.handle(queryOutboundGatewayWithTemplate("{name: 'Xavi'}", true))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewaySingleQueryExpressionFlow() {
			return f -> f
					.handle(queryExpressionOutboundGateway(true))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewayQueryExpressionFlow() {
			return f -> f
					.handle(queryExpressionOutboundGateway(false))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewayQueryExpressionLimitFlow() {
			return f -> f
					.handle(queryExpressionOutboundGateway(false, 2))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewayQueryFunctionFlow() {
			return f -> f
					.handle(queryFunctionOutboundGateway(true))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewayCollectionNameFunctionFlow() {
			return f -> f
					.handle(collectionNameFunctionOutboundGateway(true))
					.channel(getResultChannel());
		}

		@Bean
		public IntegrationFlow gatewayCollectionCallbackFlow(QueueChannel getResultChannel) {
			return f -> f
					.handle(collectionCallbackOutboundGateway(
							(collection, requestMessage) -> collection.countDocuments()))
					.channel(getResultChannel);
		}

		@Bean
		public QueueChannelSpec getResultChannel() {
			return MessageChannels.queue();
		}

		@Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return MONGO_DATABASE_FACTORY;
		}

		@Bean
		public MongoConverter mongoConverter() {
			return new TestMongoConverter(mongoDbFactory(), new MongoMappingContext());
		}

		@Bean
		public MongoOperations mongoTemplate() {
			return new MongoTemplate(mongoDbFactory());
		}

		private MongoDbOutboundGatewaySpec queryOutboundGateway(String query, boolean expectSingleResult) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.query(query)
					.collectionNameExpression("headers.collection")
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec queryOutboundGatewayWithTemplate(String query, boolean expectSingleResult) {
			return MongoDb.outboundGateway(mongoTemplate())
					.query(query)
					.collectionName(COLLECTION_NAME)
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec queryExpressionOutboundGateway(boolean expectSingleResult) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.queryExpression("headers.query")
					.collectionName(COLLECTION_NAME)
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec queryExpressionOutboundGateway(boolean expectSingleResult, int maxResults) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.queryExpression("new BasicQuery('{''address.state'' : ''PA''}').limit(" + maxResults + ")")
					.collectionName(COLLECTION_NAME)
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec queryFunctionOutboundGateway(boolean expectSingleResult) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.queryFunction(msg ->
							Query.query(Criteria.where("name")
									.is(msg.getPayload())))
					.collectionNameExpression("headers.collection")
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec collectionNameFunctionOutboundGateway(boolean expectSingleResult) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.queryExpression("headers.query")
					.<String>collectionNameFunction(Message::getPayload)
					.expectSingleResult(expectSingleResult)
					.entityClass(Person.class);
		}

		private MongoDbOutboundGatewaySpec collectionCallbackOutboundGateway(
				MessageCollectionCallback<?> collectionCallback) {

			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.collectionCallback(collectionCallback)
					.collectionName(COLLECTION_NAME)
					.entityClass(Person.class);
		}

		@Bean
		public IntegrationFlow reactiveStore() {
			return f -> f
					.channel(MessageChannels.flux())
					.handleReactive(MongoDb.reactiveOutboundChannelAdapter(REACTIVE_MONGO_DATABASE_FACTORY));
		}

	}

}
