/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mongodb.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

/**
 * @author Xavier Padró
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class MongoDbTests extends MongoDbAvailableTests {

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

	@Before
	public void setUp() throws Exception {
		createPersons();
	}

	@After
	public void cleanUp() {
		mongoTemplate.dropCollection(COLLECTION_NAME);
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithSingleQuery() {
		gatewaySingleQueryFlow.send(MessageBuilder
				.withPayload("Xavi")
				.setHeader("collection", "data")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		Person retrievedPerson = (Person) result.getPayload();
		assertEquals("Xavi", retrievedPerson.getName());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithSingleQueryWithTemplate() {
		gatewaySingleQueryWithTemplateFlow.send(MessageBuilder.withPayload("Xavi").build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		Person retrievedPerson = (Person) result.getPayload();
		assertEquals("Xavi", retrievedPerson.getName());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithSingleQueryExpression() {
		gatewaySingleQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{'name' : 'Artem'}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		Person retrievedPerson = (Person) result.getPayload();
		assertEquals("Artem", retrievedPerson.getName());
	}

	@Test(expected = ReplyRequiredException.class)
	@MongoDbAvailable
	public void testGatewayWithSingleQueryExpressionNoPersonFound() {
		gatewaySingleQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{'name' : 'NonExisting'}")
				.build());

		this.getResultChannel.receive(10_000);
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithQueryExpression() {
		gatewayQueryExpressionFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		List<Person> retrievedPersons = getPersons(result);
		assertEquals(4, retrievedPersons.size());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithQueryExpressionAndLimit() {
		gatewayQueryExpressionLimitFlow.send(MessageBuilder
				.withPayload("")
				.setHeader("query", "{}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		List<Person> retrievedPersons = getPersons(result);
		assertEquals(2, retrievedPersons.size());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithQueryFunction() {
		gatewayQueryFunctionFlow.send(MessageBuilder
				.withPayload("Gary")
				.setHeader("collection", "data")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		Person person = (Person) result.getPayload();
		assertEquals("Gary", person.getName());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithCollectionNameFunction() {
		gatewayCollectionNameFunctionFlow.send(MessageBuilder
				.withPayload("data")
				.setHeader("query", "{'name' : 'Gary'}")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		Person person = (Person) result.getPayload();
		assertEquals("Gary", person.getName());
	}

	@Test
	@MongoDbAvailable
	public void testGatewayWithCollectionCallback() {
		gatewayCollectionCallbackFlow.send(MessageBuilder
				.withPayload("")
				.build());

		Message<?> result = this.getResultChannel.receive(10_000);

		assertNotNull(result);
		long count = (Long) result.getPayload();
		assertEquals(4, count);
	}

	@SuppressWarnings("unchecked")
	private List<Person> getPersons(Message<?> message) {
		return (List<Person>) message.getPayload();
	}

	private void createPersons() {
		BulkOperations bulkOperations = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, COLLECTION_NAME);
		bulkOperations.insert(Arrays.asList(
				this.createPerson("Artem"),
				this.createPerson("Gary"),
				this.createPerson("Oleg"),
				this.createPerson("Xavi")));
		bulkOperations.execute();
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
		public IntegrationFlow gatewayCollectionCallbackFlow() {
			return f -> f
					.handle(collectionCallbackOutboundGateway(MongoCollection::count))
					.channel(getResultChannel());
		}

		@Bean
		public MessageChannel getResultChannel() {
			return MessageChannels.queue().get();
		}

		@Bean
		public MongoDbFactory mongoDbFactory() {
			return new SimpleMongoDbFactory(new MongoClient(), "test");
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

		private MongoDbOutboundGatewaySpec collectionCallbackOutboundGateway(CollectionCallback<?> collectionCallback) {
			return MongoDb.outboundGateway(mongoDbFactory(), mongoConverter())
					.collectionCallback(collectionCallback)
					.collectionName(COLLECTION_NAME)
					.entityClass(Person.class);
		}

	}

}
