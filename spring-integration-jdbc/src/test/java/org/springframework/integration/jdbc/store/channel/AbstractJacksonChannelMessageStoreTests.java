/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yoobin Yoon
 *
 * @since 7.0
 */
@DirtiesContext
public abstract class AbstractJacksonChannelMessageStoreTests {

	protected static final String TEST_MESSAGE_GROUP = "AbstractJacksonChannelMessageStoreTests";

	protected static final String REGION = "AbstractJacksonChannelMessageStoreTests";

	@Autowired
	protected DataSource dataSource;

	protected JdbcChannelMessageStore messageStore;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected ChannelMessageStoreQueryProvider queryProvider;

	@BeforeEach
	public void init() {
		this.messageStore = new JdbcChannelMessageStore(dataSource);
		this.messageStore.setRegion(REGION);
		this.messageStore.setTablePrefix("JSON_");
		this.messageStore.setChannelMessageStoreQueryProvider(queryProvider);
		this.messageStore.setPreparedStatementSetter(
				new JacksonChannelMessageStorePreparedStatementSetter());
		this.messageStore.setMessageRowMapper(
				new JacksonMessageRowMapper(getTrustedPackages()));
		this.messageStore.afterPropertiesSet();
		this.messageStore.removeMessageGroup(TEST_MESSAGE_GROUP);
	}

	protected String[] getTrustedPackages() {
		return new String[] {
				"org.springframework.integration.jdbc.store.channel"
		};
	}

	@Test
	public void testGetNonExistentMessageFromGroup() {
		Message<?> result = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertThat(result).isNull();
	}

	@Test
	public void testSimpleStringPayload() {
		Message<String> message = MessageBuilder.withPayload("Hello JSON World")
				.setHeader("customHeader", "customValue")
				.build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		Message<?> retrievedMessage = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);

		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo("Hello JSON World");
		assertThat(retrievedMessage.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
		assertThat(retrievedMessage.getHeaders().get("customHeader")).isEqualTo("customValue");
	}

	@Test
	public void testCustomPayloadSerialization() {
		TestMailMessage payload = new TestMailMessage(
				"Order Confirmation",
				"Your order has been confirmed.",
				"customer@example.com"
		);

		Message<TestMailMessage> message = MessageBuilder.withPayload(payload)
				.setHeader("orderId", "ORDER-12345")
				.build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		Message<?> retrievedMessage = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);

		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isInstanceOf(TestMailMessage.class);

		TestMailMessage retrievedPayload = (TestMailMessage) retrievedMessage.getPayload();
		assertThat(retrievedPayload).isEqualTo(payload);
		assertThat(retrievedPayload.subject()).isEqualTo("Order Confirmation");
		assertThat(retrievedPayload.body()).isEqualTo("Your order has been confirmed.");
		assertThat(retrievedPayload.to()).isEqualTo("customer@example.com");
		assertThat(retrievedMessage.getHeaders().get("orderId")).isEqualTo("ORDER-12345");
	}

	@Test
	public void testMessageHeadersPreserved() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("stringHeader", "value")
				.setHeader("intHeader", 42)
				.setHeader("longHeader", 12345L)
				.setHeader("booleanHeader", true)
				.build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		Message<?> retrievedMessage = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);

		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getHeaders().get("stringHeader")).isEqualTo("value");
		assertThat(retrievedMessage.getHeaders().get("intHeader")).isEqualTo(42);
		assertThat(retrievedMessage.getHeaders().get("longHeader")).isEqualTo(12345L);
		assertThat(retrievedMessage.getHeaders().get("booleanHeader")).isEqualTo(true);
	}

	@Test
	public void testJsonStructureInDatabase() {
		TestMailMessage payload = new TestMailMessage(
				"Test Subject",
				"Test Body",
				"test@example.com"
		);
		Message<TestMailMessage> message = MessageBuilder.withPayload(payload).build();

		UUID messageId = message.getHeaders().getId();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String storedJson = jdbcTemplate.queryForObject(
				"SELECT MESSAGE_CONTENT FROM JSON_CHANNEL_MESSAGE WHERE MESSAGE_ID = ? AND REGION = ?",
				String.class,
				messageId.toString(),
				REGION
		);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(storedJson);

		assertThat(jsonNode.has("@class")).isTrue();
		assertThat(jsonNode.get("@class").asString())
				.isEqualTo("org.springframework.messaging.support.GenericMessage");

		JsonNode payloadNode = jsonNode.get("payload");
		assertThat(payloadNode.get("@class").asString())
				.isEqualTo("org.springframework.integration.jdbc.store.channel.TestMailMessage");
		assertThat(payloadNode.get("subject").asString()).isEqualTo("Test Subject");
		assertThat(payloadNode.get("body").asString()).isEqualTo("Test Body");
		assertThat(payloadNode.get("to").asString()).isEqualTo("test@example.com");

		JsonNode headersNode = jsonNode.get("headers");
		assertThat(headersNode.has("id")).isTrue();
		assertThat(headersNode.has("timestamp")).isTrue();
	}

	@Test
	public void testMultipleMessagesInGroup() {
		Message<String> message1 = MessageBuilder.withPayload("Message 1").build();
		Message<String> message2 = MessageBuilder.withPayload("Message 2").build();
		Message<String> message3 = MessageBuilder.withPayload("Message 3").build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) -> {
			this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message1);
			this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message2);
			this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message3);
		});

		Message<?> retrieved1 = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		Message<?> retrieved2 = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		Message<?> retrieved3 = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);

		assertThat(retrieved1.getPayload()).isEqualTo("Message 1");
		assertThat(retrieved2.getPayload()).isEqualTo("Message 2");
		assertThat(retrieved3.getPayload()).isEqualTo("Message 3");
	}

	@Test
	public void testAddAndGetWithDifferentRegion() {
		Message<String> message = MessageBuilder.withPayload("foo").build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		this.messageStore.setRegion("DIFFERENT_REGION");
		this.messageStore.afterPropertiesSet();

		Message<?> result = this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertThat(result).isNull();

		this.messageStore.setRegion(REGION);
		this.messageStore.afterPropertiesSet();
	}

	@Test
	public void testJsonContainsAllMessageHeaders() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("header1", "value1")
				.setHeader("header2", 123)
				.build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult((status) ->
				this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String storedJson = jdbcTemplate.queryForObject(
				"SELECT MESSAGE_CONTENT FROM JSON_CHANNEL_MESSAGE WHERE MESSAGE_ID = ?",
				String.class,
				message.getHeaders().getId().toString()
		);

		assertThat(storedJson).contains("\"header1\"");
		assertThat(storedJson).contains("\"value1\"");
		assertThat(storedJson).contains("\"header2\"");
		assertThat(storedJson).contains("123");
		assertThat(storedJson).contains("\"id\"");
		assertThat(storedJson).contains("\"timestamp\"");
	}

}
