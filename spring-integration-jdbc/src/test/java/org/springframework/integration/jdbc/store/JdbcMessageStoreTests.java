/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.jdbc.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Will Schipp
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
@Transactional
public class JdbcMessageStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
	}

	@Test
	public void testGetNonExistent() throws Exception {
		Message<?> result = messageStore.getMessage(UUID.randomUUID());
		assertNull(result);
	}

	@Test
	public void testAddAndGet() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertThat(saved, sameExceptIgnorableHeaders(result));
	}

	@Test
	public void testWithMessageHistory() throws Exception {

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
	public void testSize() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessage(message);
		assertEquals(1, messageStore.getMessageCount());
	}

	@Test
	public void testSerializer() throws Exception {
		// N.B. these serializers are not realistic (just for test purposes)
		messageStore.setSerializer((object, outputStream) -> {
			outputStream.write(((Message<?>) object).getPayload().toString().getBytes());
			outputStream.flush();
		});
		messageStore.setDeserializer(inputStream -> {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			return new GenericMessage<String>(reader.readLine());
		});
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertNotNull(messageStore.getMessage(message.getHeaders().getId()));
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testAddAndGetWithDifferentRegion() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		messageStore.setRegion("FOO");
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNull(result);
	}

	@Test
	public void testAddAndUpdate() throws Exception {
		Message<?> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.addMessage(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.addMessage(message);
		message = messageStore.getMessage(message.getHeaders().getId());
		assertEquals("Y", new IntegrationMessageHeaderAccessor(message).getCorrelationId());
	}

	@Test
	public void testAddAndUpdateAlreadySaved() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertEquals(message, result);
	}

	@Test
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
	public void testAddAndUpdateWithChange() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).setHeader("newHeader", 1).build();
		Message<String> result = messageStore.addMessage(copy);
		assertNotSame(saved, result);
		assertThat(saved, sameExceptIgnorableHeaders(result, "newHeader"));
		assertNotNull(messageStore.getMessage(saved.getHeaders().getId()));
	}

	@Test
	public void testAddAndRemoveMessageGroup() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		assertNotNull(messageStore.removeMessage(message.getHeaders().getId()));
	}

	@Test
	public void testAddAndGetMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		long now = System.currentTimeMillis();
		messageStore.addMessagesToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());
		assertTrue("Timestamp too early: " + group.getTimestamp() + "<" + now, group.getTimestamp() >= now);
	}

	@Test
	public void testAddAndRemoveMessageFromMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.removeMessagesFromGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Test
	public void testAddAndRemoveMessagesFromMessageGroup() throws Exception {
		String groupId = "X";
		this.messageStore.setRemoveBatchSize(10);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messages.add(message);
		}
		this.messageStore.addMessagesToGroup(groupId, messages.toArray(new Message<?>[messages.size()]));
		MessageGroup group = this.messageStore.getMessageGroup(groupId);
		assertEquals(25, group.size());
		this.messageStore.removeMessagesFromGroup(groupId, messages);
		group = this.messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Test
	public void testRemoveMessageGroup() throws Exception {
		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();
		String groupId = "X";

		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.removeMessageGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());

		String uuidGroupId = UUIDConverter.getUUID(groupId).toString();
		assertTrue(template.queryForList(
				"SELECT * from INT_GROUP_TO_MESSAGE where GROUP_KEY = ?", uuidGroupId).size() == 0);
	}

	@Test
	public void testCompleteMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.completeGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertTrue(group.isComplete());
		assertEquals(1, group.size());
	}

	@Test
	public void testUpdateLastReleasedSequence() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.setLastReleasedSequenceNumberForGroup(groupId, 5);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(5, group.getLastReleasedMessageSequenceNumber());
	}

	@Test
	public void testMessageGroupCount() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessagesToGroup(groupId, message);
		assertEquals(1, messageStore.getMessageGroupCount());
	}

	@Test
	public void testMessageGroupSizes() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessagesToGroup(groupId, message);
		assertEquals(1, messageStore.getMessageCountForAllMessageGroups());
	}

	@Test
	public void testOrderInMessageGroup() throws Exception {
		String groupId = "X";

		this.messageStore.addMessagesToGroup(groupId,
				MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(1);
		this.messageStore.addMessagesToGroup(groupId,
				MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		MessageGroup group = this.messageStore.getMessageGroup(groupId);
		assertEquals(2, group.size());
		assertEquals("foo", this.messageStore.pollMessageFromGroup(groupId).getPayload());
		assertEquals("bar", this.messageStore.pollMessageFromGroup(groupId).getPayload());
	}

	@Test
	public void testExpireMessageGroupOnCreateOnly() throws Exception {
		final String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		final CountDownLatch groupRemovalLatch = new CountDownLatch(1);
		messageStore.registerMessageGroupExpiryCallback((messageGroupStore, group) -> {
			messageGroupStore.removeMessageGroup(group.getGroupId());
			groupRemovalLatch.countDown();
		});

		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();

		template.update("UPDATE INT_MESSAGE_GROUP set CREATED_DATE=? where GROUP_KEY=? and REGION=?",
				(PreparedStatementSetter) ps -> {
					ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - 10000));
					ps.setString(2, UUIDConverter.getUUID(groupId).toString());
					ps.setString(3, "DEFAULT");
				});

		messageStore.expireMessageGroups(2000);

		group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
		assertTrue(groupRemovalLatch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testExpireMessageGroupOnIdleOnly() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.setTimeoutOnIdle(true);
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback((messageGroupStore, group) -> messageGroupStore.removeMessageGroup(group.getGroupId()));

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();

		updateMessageGroup(template, groupId, 1000);

		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(1, group.size());

		updateMessageGroup(template, groupId, 2000);

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		group = messageStore.getMessageGroup(groupId);
		assertEquals(2, group.size());

		updateMessageGroup(template, groupId, 2000);

		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	private void updateMessageGroup(JdbcTemplate template, final String groupId, final long timeout) {
		template.update("UPDATE INT_MESSAGE_GROUP set UPDATED_DATE=? where GROUP_KEY=? and REGION=?",
				(PreparedStatementSetter) ps -> {
					ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - timeout));
					ps.setString(2, UUIDConverter.getUUID(groupId).toString());
					ps.setString(3, "DEFAULT");
				});

	}

	@Test
	public void testMessagePollingFromTheGroup() throws Exception {
		String groupId = "X";

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(10);
		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		Thread.sleep(10);
		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("baz").setCorrelationId(groupId).build());

		messageStore.addMessagesToGroup("Y", MessageBuilder.withPayload("barA").setCorrelationId(groupId).build(),
				MessageBuilder.withPayload("bazA").setCorrelationId(groupId).build());

		MessageGroup group = messageStore.getMessageGroup("X");
		assertEquals(3, group.size());

		Message<?> message1 = messageStore.pollMessageFromGroup("X");
		assertNotNull(message1);
		assertEquals("foo", message1.getPayload());

		group = messageStore.getMessageGroup("X");
		assertEquals(2, group.size());

		Message<?> message2 = messageStore.pollMessageFromGroup("X");
		assertNotNull(message2);
		assertEquals("bar", message2.getPayload());

		group = messageStore.getMessageGroup("X");
		assertEquals(1, group.size());
	}

	@Test
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

		assertEquals(1, messageFromGroup1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));
		assertEquals(2, messageFromGroup2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));

	}

	@Test
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

		assertEquals(1, messageFromRegion1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));
		assertEquals(2, messageFromRegion2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER));

	}

	@Test
	public void testCompletedNotExpiredGroupINT3037() throws Exception {
		/*
		 * based on the aggregator scenario as follows;
		 *
		 * send three messages in
		 * 1 of 2
		 * 2 of 2
		 * 2 of 2 (last again)
		 *
		 * expected behavior is that the LAST message (2 of 2 repeat) should be on the discard channel
		 * (discard behavior performed by the AbstractCorrelatingMessageHandler.handleMessageInternal)
		 */
		final JdbcMessageStore messageStore = new JdbcMessageStore(dataSource);
		//init
		String groupId = "group";
		//build the messages
		Message<?> oneOfTwo = MessageBuilder.withPayload("hello")
				.setSequenceNumber(1)
				.setSequenceSize(2)
				.setCorrelationId(groupId)
				.build();
		Message<?> twoOfTwo = MessageBuilder.withPayload("world")
				.setSequenceNumber(2)
				.setSequenceSize(2)
				.setCorrelationId(groupId)
				.build();
		//add to the messageStore
		messageStore.addMessagesToGroup(groupId, oneOfTwo, twoOfTwo);
		//check that 2 messages are there
		assertTrue(messageStore.getMessageGroupCount() == 1);
		assertTrue(messageStore.getMessageCount() == 2);
		//retrieve the group (like in the aggregator)
		MessageGroup messageGroup = messageStore.getMessageGroup(groupId);
		//'complete' the group
		messageStore.completeGroup(messageGroup.getGroupId());
		//now clear the messages
		for (Message<?> message : messageGroup.getMessages()) {
			messageStore.removeMessagesFromGroup(groupId, message);
		} //end for
		//'add' the other message --> emulated by getting the messageGroup
		messageGroup = messageStore.getMessageGroup(groupId);
		//should be marked 'complete' --> old behavior it would not
		assertTrue(messageGroup.isComplete());
	}

}
