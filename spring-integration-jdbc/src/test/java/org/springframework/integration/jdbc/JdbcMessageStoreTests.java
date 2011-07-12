/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupCallback;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@Before
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
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
		assertNull(messageStore.getMessage(message.getHeaders().getId()));
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertNotNull(result);
		assertThat(saved, sameExceptIgnorableHeaders(result));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.SAVED_KEY));
		assertNotNull(result.getHeaders().get(JdbcMessageStore.CREATED_DATE_KEY));
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
		assertNull(messageStore.getMessage(message.getHeaders().getId()));
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
		assertEquals("Y", messageStore.getMessage(message.getHeaders().getId()).getHeaders().getCorrelationId());
	}

	@Test
	@Transactional
	public void testAddAndUpdateAlreadySaved() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertEquals(message, result);
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
		assertNotSame(copy, result);
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
	public void testMarkedMessageGroupSizes() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessageToGroup(groupId, message);
		assertEquals(0, messageStore.getMarkedMessageCountForAllMessageGroups());
		messageStore.markMessageGroup(messageStore.getMessageGroup(groupId));
		assertEquals(1, messageStore.getMarkedMessageCountForAllMessageGroups());
	}

	@Test
	@Transactional
	public void testOrderInMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		message = MessageBuilder.withPayload("bar").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(2, group.size());
		Iterator<Message<?>> iterator = group.getUnmarked().iterator();
		assertEquals("foo", iterator.next().getPayload());
		assertEquals("bar", iterator.next().getPayload());
	}

	@Test
	@Transactional
	public void testAddAndMarkMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		group = messageStore.markMessageGroup(group);
		assertEquals(1, group.getMarked().size());
	}

	@Test
	@Transactional
	public void testAddAndMarkMessageInGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.addMessageToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		MessageGroup group = messageStore.markMessageFromGroup(groupId, message);
		assertEquals(1, group.getMarked().size());
	}

	@Test
	@Transactional
	public void testExpireMessageGroup() throws Exception {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessageToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});
		messageStore.expireMessageGroups(-10000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

}
