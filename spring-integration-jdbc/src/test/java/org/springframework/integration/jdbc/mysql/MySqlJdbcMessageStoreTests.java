/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jdbc.JdbcMessageStore;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Based on the test for Derby:
 *
 * {@link org.springframework.integration.jdbc.JdbcMessageStoreTests}
 *
 * This tests requires at least MySql 5.6.4 as it uses the fractional second support
 * in that version. For more information, please see:
 *
 * http://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html
 *
 * Also, please make sure you are using the respective DDL scripts:
 *
 * schema-mysql-5_6_4.sql
 *
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@Ignore
public class MySqlJdbcMessageStoreTests {

	private static final Log LOG = LogFactory.getLog(MySqlJdbcMessageStoreTests.class);

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
		messageStore.setRegion("JdbcMessageStoreTests");
	}

	@After
	public void afterTest() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		new TransactionTemplate(this.transactionManager).execute(new TransactionCallback<Void>() {
		public Void doInTransaction(TransactionStatus status) {
			final int deletedGroupToMessageRows = jdbcTemplate.update("delete from INT_GROUP_TO_MESSAGE");
			final int deletedMessages = jdbcTemplate.update("delete from INT_MESSAGE");
			final int deletedMessageGroups = jdbcTemplate.update("delete from INT_MESSAGE_GROUP");

			LOG.info(String.format("Cleaning Database - Deleted Messages: %s, " +
					"Deleted GroupToMessage Rows: %s, Deleted Message Groups: %s",
					deletedMessages, deletedGroupToMessageRows, deletedMessageGroups));
			return null;
		}
		});
	}

	@Test
	@Transactional
	public void testGetNonExistent() throws Exception {
		Message<?> result = messageStore.getMessage(UUID.randomUUID());
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndGet() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertNotNull(messageStore.getMessage(message.getHeaders().getId()));
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertThat(saved, sameExceptIgnorableHeaders(result));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.SAVED_KEY));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.CREATED_DATE_KEY));
	}

	@Test
	@Transactional
	public void testWithMessageHistory() throws Exception{

		Message<?> message = new GenericMessage<String>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		messageStore.addMessage(message);
		message = messageStore.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}

	@Test
	@Transactional
	public void testSize() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessage(message);
		assertEquals(1, messageStore.getMessageCount());
	}

	@Test
	@Transactional
	public void testSerializer() throws Exception {
		// N.B. these serializers are not realistic (just for test purposes)
		messageStore.setSerializer(new Serializer<Message<?>>() {
			public void serialize(Message<?> object, OutputStream outputStream) throws IOException {
				outputStream.write(((Message<?>) object).getPayload().toString().getBytes());
				outputStream.flush();
			}
		});
		messageStore.setDeserializer(new Deserializer<GenericMessage<String>>() {
			public GenericMessage<String> deserialize(InputStream inputStream) throws IOException {
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				return new GenericMessage<String>(reader.readLine());
			}
		});
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertNotNull(messageStore.getMessage(message.getHeaders().getId()));
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
	}

	@Test
	@Transactional
	public void testAddAndGetWithDifferentRegion() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		messageStore.setRegion("FOO");
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNull(result);
	}

	@Test
	@Transactional
	public void testAddAndUpdate() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.addMessage(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.addMessage(message);
		assertEquals("Y", new IntegrationMessageHeaderAccessor(messageStore.getMessage(message.getHeaders().getId())).getCorrelationId());
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySaved() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertSame(message, result);
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySavedAndCopied() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).build();
		Message<String> result = messageStore.addMessage(copy);
		assertEquals(copy, result);
		assertEquals(saved, result);
		assertNotNull(messageStore.getMessage(saved.getHeaders().getId()));
	}

	@Test
	@Transactional
	public void testAddAndUpdateWithChange() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).setHeader("newHeader", 1).build();
		Message<String> result = messageStore.addMessage(copy);
		assertNotSame(saved, result);
		assertThat(saved, sameExceptIgnorableHeaders(result, JdbcMessageStore.CREATED_DATE_KEY, "newHeader"));
		assertNotNull(messageStore.getMessage(saved.getHeaders().getId()));
	}

	@Test
	@Transactional
	public void testAddAndRemoveMessageGroup() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		assertNotNull(messageStore.removeMessage(message.getHeaders().getId()));
	}

	@Test
	@Transactional
	public void testAddAndGetMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		long now = System.currentTimeMillis();
		messageStore.addMessageToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());
		assertTrue("Timestamp too early: " + group.getTimestamp() + "<" + now, group.getTimestamp() >= now);
	}

	@Test
	@Transactional
	public void testAddAndRemoveMessageFromMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.removeMessageFromGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Test
	@Transactional
	public void testRemoveMessageGroup() throws Exception {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		template.afterPropertiesSet();
		String groupId = "X";

		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.removeMessageGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());

		String uuidGroupId = UUIDConverter.getUUID(groupId).toString();
		assertTrue(template.queryForList(
				"SELECT * from INT_GROUP_TO_MESSAGE where GROUP_KEY = '"  + uuidGroupId + "'").size() == 0);
	}

	@Test
	@Transactional
	public void testCompleteMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.completeGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertTrue(group.isComplete());
		assertEquals(1, group.size());
	}

	@Test
	@Transactional
	public void testUpdateLastReleasedSequence() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.setLastReleasedSequenceNumberForGroup(groupId, 5);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(5, group.getLastReleasedMessageSequenceNumber());
	}

	@Test
	@Transactional
	public void testMessageGroupCount() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessageToGroup(groupId, message);
		assertEquals(1, messageStore.getMessageGroupCount());
	}

	@Test
	@Transactional
	public void testMessageGroupSizes() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessageToGroup(groupId, message);
		assertEquals(1, messageStore.getMessageCountForAllMessageGroups());
	}

	@Test
	@Transactional
	public void testOrderInMessageGroup() throws Exception {
		String groupId = "X";

		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(1);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(2, group.size());
		assertEquals("foo", messageStore.pollMessageFromGroup(groupId).getPayload());
		assertEquals("bar", messageStore.pollMessageFromGroup(groupId).getPayload());
	}

	@Test
	@Transactional
	public void testExpireMessageGroupOnCreateOnly() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});
		Thread.sleep(1000);
		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		Thread.sleep(2001);
		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Test
	@Transactional
	public void testExpireMessageGroupOnIdleOnly() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.setTimeoutOnIdle(true);
		messageStore.addMessageToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});
		Thread.sleep(1000);
		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());
		Thread.sleep(2000);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		group = messageStore.getMessageGroup(groupId);
		assertEquals(2, group.size());
		Thread.sleep(2000);
		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
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
		assertEquals(3, group.size());

		Message<?> message1 = messageStore.pollMessageFromGroup(groupX);
		assertNotNull(message1);
		assertEquals("foo", message1.getPayload());

		group = messageStore.getMessageGroup(groupX);
		assertEquals(2, group.size());

		Message<?> message2 = messageStore.pollMessageFromGroup(groupX);
		assertNotNull(message2);
		assertEquals("bar", message2.getPayload());

		group = messageStore.getMessageGroup(groupX);
		assertEquals(1, group.size());
	}

	@Test
	@Transactional
	@Rollback(false)
	@Repeat(20)
	public void testSameMessageToMultipleGroups() throws Exception {

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

		assertNotNull(messageFromGroup1);
		assertNotNull(messageFromGroup2);

		LOG.info("messageFromGroup1: " + messageFromGroup1.getHeaders().getId() + "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromGroup1).getSequenceNumber());
		LOG.info("messageFromGroup2: " + messageFromGroup2.getHeaders().getId() + "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromGroup2).getSequenceNumber());

		assertEquals(Integer.valueOf(1), (Integer) messageFromGroup1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));
		assertEquals(Integer.valueOf(2), (Integer) messageFromGroup2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));

	}

	@Test
	@Transactional
	@Rollback(false)
	@Repeat(20)
	public void testSameMessageAndGroupToMultipleRegions() throws Exception {

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

		assertNotNull(messageFromRegion1);
		assertNotNull(messageFromRegion2);

		LOG.info("messageFromRegion1: " + messageFromRegion1.getHeaders().getId() + "; Sequence #: " + new IntegrationMessageHeaderAccessor(messageFromRegion1).getSequenceNumber());
		LOG.info("messageFromRegion2: " + messageFromRegion2.getHeaders().getId() + "; Sequence #: " +new IntegrationMessageHeaderAccessor(messageFromRegion2).getSequenceNumber());

		assertEquals(Integer.valueOf(1), (Integer) messageFromRegion1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));
		assertEquals(Integer.valueOf(2), (Integer) messageFromRegion2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));

	}
}
