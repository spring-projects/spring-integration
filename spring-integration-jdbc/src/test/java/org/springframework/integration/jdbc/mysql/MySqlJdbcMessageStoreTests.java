/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MySqlJdbcMessageStoreTests implements MySqlContainerTest {

	private static final Log LOG = LogFactory.getLog(MySqlJdbcMessageStoreTests.class);

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
		messageStore.setRegion("JdbcMessageStoreTests");
	}

	@AfterEach
	public void afterTest() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		new TransactionTemplate(this.transactionManager).execute(status -> {
			final int deletedGroupToMessageRows = jdbcTemplate.update("delete from INT_GROUP_TO_MESSAGE");
			final int deletedMessages = jdbcTemplate.update("delete from INT_MESSAGE");
			final int deletedMessageGroups = jdbcTemplate.update("delete from INT_MESSAGE_GROUP");

			LOG.info(String.format("Cleaning Database - Deleted Messages: %s, " +
							"Deleted GroupToMessage Rows: %s, Deleted Message Groups: %s",
					deletedMessages, deletedGroupToMessageRows, deletedMessageGroups));
			return null;
		});
	}

	@Test
	@Transactional
	public void testGetNonExistent() {
		Message<?> result = messageStore.getMessage(UUID.randomUUID());
		assertThat(result).isNull();
	}

	@Test
	@Transactional
	public void testAddAndGet() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNotNull();
		assertThat(saved).matches(new MessagePredicate(result));
	}

	@Test
	@Transactional
	public void testWithMessageHistory() {
		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		messageStore.addMessage(message);
		message = messageStore.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory.size()).isEqualTo(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory.get("name")).isEqualTo("fooChannel");
		assertThat(fooChannelHistory.get("type")).isEqualTo("channel");
	}

	@Test
	@Transactional
	public void testSize() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessage(message);
		assertThat(messageStore.getMessageCount()).isEqualTo(1);
	}

	@Test
	@Transactional
	public void testSerializer() {
		// N.B. these serializers are not realistic (just for test purposes)
		messageStore.setSerializer((object, outputStream) -> {
			outputStream.write(object.getPayload().toString().getBytes());
			outputStream.flush();
		});
		messageStore.setDeserializer(inputStream -> {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			return new GenericMessage<>(reader.readLine());
		});
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertThat(messageStore.getMessage(message.getHeaders().getId())).isNotNull();
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	@Transactional
	public void testAddAndGetWithDifferentRegion() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		messageStore.setRegion("FOO");
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNull();
	}

	@Test
	@Transactional
	public void testAddAndUpdate() {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.addMessage(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.addMessage(message);
		assertThat(new IntegrationMessageHeaderAccessor(messageStore.getMessage(message.getHeaders().getId()))
				.getCorrelationId()).isEqualTo("Y");
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySaved() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertThat(result).isEqualTo(message);
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySavedAndCopied() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).build();
		Message<String> result = messageStore.addMessage(copy);
		assertThat(result).isEqualTo(copy);
		assertThat(result).isEqualTo(saved);
		assertThat(messageStore.getMessage(saved.getHeaders().getId())).isNotNull();
	}

	@Test
	@Transactional
	public void testAddAndUpdateWithChange() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).setHeader("newHeader", 1).build();
		Message<String> result = messageStore.addMessage(copy);
		assertThat(result).isNotSameAs(saved);
		assertThat(saved).matches(new MessagePredicate(result, "newHeader"));
		assertThat(messageStore.getMessage(saved.getHeaders().getId())).isNotNull();
	}

	@Test
	@Transactional
	public void testAddAndRemoveMessageGroup() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		assertThat(messageStore.removeMessage(message.getHeaders().getId())).isNotNull();
	}

	@Test
	@Transactional
	public void testAddAndGetMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		long now = System.currentTimeMillis();
		messageStore.addMessageToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);
		assertThat(group.getTimestamp() >= now).as("Timestamp too early: " + group.getTimestamp() + "<" + now).isTrue();
	}

	@Test
	@Transactional
	public void testAddAndRemoveMessageFromMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.removeMessagesFromGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	@Test
	@Transactional
	public void testRemoveMessageGroup() {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		template.afterPropertiesSet();
		String groupId = "X";

		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.removeMessageGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);

		String uuidGroupId = UUIDConverter.getUUID(groupId).toString();
		assertThat(template.queryForList(
				"SELECT * from INT_GROUP_TO_MESSAGE where GROUP_KEY = '" + uuidGroupId + "'").size() == 0).isTrue();
	}

	@Test
	@Transactional
	public void testCompleteMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.completeGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.isComplete()).isTrue();
		assertThat(group.size()).isEqualTo(1);
	}

	@Test
	@Transactional
	public void testUpdateLastReleasedSequence() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.setLastReleasedSequenceNumberForGroup(groupId, 5);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.getLastReleasedMessageSequenceNumber()).isEqualTo(5);
	}

	@Test
	@Transactional
	public void testMessageGroupCount() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessageToGroup(groupId, message);
		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
	}

	@Test
	@Transactional
	public void testMessageGroupSizes() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessageToGroup(groupId, message);
		assertThat(messageStore.getMessageCountForAllMessageGroups()).isEqualTo(1);
	}

	@Test
	@Transactional
	public void testOrderInMessageGroup() throws Exception {
		String groupId = "X";

		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(1);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(2);
		assertThat(messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("foo");
		assertThat(messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("bar");
	}

	@Test
	@Transactional
	public void testExpireMessageGroupOnCreateOnly() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback(
				(messageGroupStore, group) -> messageGroupStore.removeMessageGroup(group.getGroupId()));
		Thread.sleep(1000);
		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		Thread.sleep(2001);
		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	@Test
	@Transactional
	public void testExpireMessageGroupOnIdleOnly() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.setTimeoutOnIdle(true);
		messageStore.addMessageToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback(
				(messageGroupStore, group) -> messageGroupStore.removeMessageGroup(group.getGroupId()));
		Thread.sleep(1000);
		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);
		Thread.sleep(2000);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(2);
		Thread.sleep(2000);
		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	@Test
	@Transactional
	public void testMessagePollingFromTheGroup() throws Exception {

		final String groupX = "X";

		messageStore.addMessageToGroup(groupX, MessageBuilder.withPayload("foo").setCorrelationId(groupX).build());
		Thread.sleep(100);
		messageStore.addMessageToGroup(groupX, MessageBuilder.withPayload("bar").setCorrelationId(groupX).build());
		Thread.sleep(100);
		messageStore.addMessageToGroup(groupX, MessageBuilder.withPayload("baz").setCorrelationId(groupX).build());
		Thread.sleep(100);
		messageStore.addMessageToGroup("Y", MessageBuilder.withPayload("barA").setCorrelationId(groupX).build());
		Thread.sleep(100);
		messageStore.addMessageToGroup("Y", MessageBuilder.withPayload("bazA").setCorrelationId(groupX).build());
		Thread.sleep(100);
		MessageGroup group = messageStore.getMessageGroup(groupX);
		assertThat(group.size()).isEqualTo(3);

		Message<?> message1 = messageStore.pollMessageFromGroup(groupX);
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("foo");

		group = messageStore.getMessageGroup(groupX);
		assertThat(group.size()).isEqualTo(2);

		Message<?> message2 = messageStore.pollMessageFromGroup(groupX);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("bar");

		group = messageStore.getMessageGroup(groupX);
		assertThat(group.size()).isEqualTo(1);
	}

	@Test
	@Transactional
	@Rollback(false)
	@Repeat(20)
	public void testSameMessageToMultipleGroups() {
		final String group1Id = "group1";
		final String group2Id = "group2";

		final Message<String> message = MessageBuilder.withPayload("foo").build();

		final MessageBuilder<String> builder1 = MessageBuilder.fromMessage(message);
		final MessageBuilder<String> builder2 = MessageBuilder.fromMessage(message);

		builder1.setSequenceNumber(1);
		builder2.setSequenceNumber(2);

		final Message<?> message1 = builder1.build();
		final Message<?> message2 = builder2.build();

		messageStore.addMessageToGroup(group1Id, message1);
		messageStore.addMessageToGroup(group2Id, message2);

		final Message<?> messageFromGroup1 = messageStore.pollMessageFromGroup(group1Id);
		final Message<?> messageFromGroup2 = messageStore.pollMessageFromGroup(group2Id);

		assertThat(messageFromGroup1).isNotNull();
		assertThat(messageFromGroup2).isNotNull();

		LOG.info("messageFromGroup1: " + messageFromGroup1.getHeaders().getId()
				+ "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromGroup1).getSequenceNumber());
		LOG.info("messageFromGroup2: " + messageFromGroup2.getHeaders().getId()
				+ "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromGroup2).getSequenceNumber());

		assertThat(messageFromGroup1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.isEqualTo(1);
		assertThat(messageFromGroup2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.isEqualTo(2);

	}

	@Test
	@Transactional
	@Rollback(false)
	@Repeat(20)
	public void testSameMessageAndGroupToMultipleRegions() {
		final String groupId = "myGroup";
		final String region1 = "region1";
		final String region2 = "region2";

		final JdbcMessageStore messageStore1 = new JdbcMessageStore(dataSource);
		messageStore1.setRegion(region1);

		final JdbcMessageStore messageStore2 = new JdbcMessageStore(dataSource);
		messageStore1.setRegion(region2);

		final Message<String> message = MessageBuilder.withPayload("foo").build();

		final MessageBuilder<String> builder1 = MessageBuilder.fromMessage(message);
		final MessageBuilder<String> builder2 = MessageBuilder.fromMessage(message);

		builder1.setSequenceNumber(1);
		builder2.setSequenceNumber(2);

		final Message<?> message1 = builder1.build();
		final Message<?> message2 = builder2.build();

		messageStore1.addMessageToGroup(groupId, message1);
		messageStore2.addMessageToGroup(groupId, message2);

		final Message<?> messageFromRegion1 = messageStore1.pollMessageFromGroup(groupId);
		final Message<?> messageFromRegion2 = messageStore2.pollMessageFromGroup(groupId);

		assertThat(messageFromRegion1).isNotNull();
		assertThat(messageFromRegion2).isNotNull();

		LOG.info("messageFromRegion1: " + messageFromRegion1.getHeaders().getId()
				+ "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromRegion1).getSequenceNumber());
		LOG.info("messageFromRegion2: " + messageFromRegion2.getHeaders().getId()
				+ "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromRegion2).getSequenceNumber());

		assertThat(messageFromRegion1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.isEqualTo(1);
		assertThat(messageFromRegion2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.isEqualTo(2);
	}

	@Test
	public void testMessageGroupCondition() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		this.messageStore.addMessagesToGroup(groupId, message);
		this.messageStore.setGroupCondition(groupId, "testCondition");
		assertThat(this.messageStore.getMessageGroup(groupId).getCondition()).isEqualTo("testCondition");
	}

	@Test
	public void sameMessageInTwoGroupsNotRemovedByFirstGroup() {
		GenericMessage<String> testMessage = new GenericMessage<>("test data");

		messageStore.addMessageToGroup("1", testMessage);
		messageStore.addMessageToGroup("2", testMessage);

		messageStore.removeMessageGroup("1");

		assertThat(messageStore.getMessageCount()).isEqualTo(1);

		messageStore.removeMessageGroup("2");

		assertThat(messageStore.getMessageCount()).isEqualTo(0);
	}

	@Test
	public void removeMessagesFromGroupDontRemoveSameMessageInOtherGroup() {
		GenericMessage<String> testMessage = new GenericMessage<>("test data");

		messageStore.addMessageToGroup("1", testMessage);
		messageStore.addMessageToGroup("2", testMessage);

		messageStore.removeMessagesFromGroup("1", testMessage);

		assertThat(messageStore.getMessageCount()).isEqualTo(1);
		assertThat(messageStore.messageGroupSize("1")).isEqualTo(0);
		assertThat(messageStore.messageGroupSize("2")).isEqualTo(1);
	}

	@Configuration
	public static class Config {

		@Value("org/springframework/integration/jdbc/schema-mysql.sql")
		Resource createSchemaScript;

		@Value("org/springframework/integration/jdbc/schema-drop-mysql.sql")
		Resource dropSchemaScript;

		@Bean
		DataSource dataSource() {
			return MySqlContainerTest.dataSource();
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSourceInitializer dataSourceInitializer() {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource());
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(this.createSchemaScript));
			dataSourceInitializer.setDatabaseCleaner(new ResourceDatabasePopulator(this.dropSchemaScript));
			return dataSourceInitializer;
		}

	}

}
