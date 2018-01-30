/*
 * Copyright 2007-2018 the original author or authors.
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

package org.springframework.integration.redis.store;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.AssertionFailedError;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public class RedisMessageGroupStoreTests extends RedisAvailableTests {

	private final UUID groupId = UUID.randomUUID();

	@Before
	@After
	public void setUpTearDown() {
		StringRedisTemplate template = createStringRedisTemplate(getConnectionFactoryForTest());
		template.delete(template.keys("MESSAGE_GROUP_*"));
	}

	@Test
	@RedisAvailable
	public void testNonExistingEmptyMessageGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof SimpleMessageGroup);
		assertEquals(0, messageGroup.size());
	}

	@Test
	@RedisAvailable
	public void testMessageGroupUpdatedDateChangesWithEachAddedMessage() throws Exception {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		Message<?> message = new GenericMessage<>("Hello");
		MessageGroup messageGroup = store.addMessageToGroup(this.groupId, message);
		assertEquals(1, messageGroup.size());
		long createdTimestamp = messageGroup.getTimestamp();
		long updatedTimestamp = messageGroup.getLastModified();
		assertEquals(createdTimestamp, updatedTimestamp);
		Thread.sleep(10);
		message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(this.groupId, message);
		createdTimestamp = messageGroup.getTimestamp();
		updatedTimestamp = messageGroup.getLastModified();
		assertTrue(updatedTimestamp > createdTimestamp);

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(2, messageGroup.size());
	}

	@Test
	@RedisAvailable
	public void testMessageGroupWithAddedMessage() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		Message<?> message = new GenericMessage<>("Hello");
		MessageGroup messageGroup = store.addMessageToGroup(this.groupId, message);
		assertEquals(1, messageGroup.size());

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(1, messageGroup.size());
	}

	@Test
	@RedisAvailable
	public void testRemoveMessageGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		assertEquals(1, messageGroup.size());

		store.removeMessageGroup(this.groupId);
		MessageGroup messageGroupA = store.getMessageGroup(this.groupId);
		assertNotSame(messageGroup, messageGroupA);
//		assertEquals(0, messageGroupA.getMarked().size());
		assertEquals(0, messageGroupA.getMessages().size());
		assertEquals(0, messageGroupA.size());

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(this.groupId);

		assertEquals(0, messageGroup.getMessages().size());
		assertEquals(0, messageGroup.size());
	}

	@Test
	@RedisAvailable
	public void testCompleteMessageGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(this.groupId);
		assertTrue(messageGroup.isComplete());
	}

	@Test
	@RedisAvailable
	public void testLastReleasedSequenceNumber() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(5, messageGroup.getLastReleasedMessageSequenceNumber());
	}

	@Test
	@RedisAvailable
	public void testRemoveMessageFromTheGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("2");
		store.addMessagesToGroup(messageGroup.getGroupId(), new GenericMessage<>("1"), message);
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), new GenericMessage<>("3"));
		assertEquals(3, messageGroup.size());

		store.removeMessagesFromGroup(this.groupId, message);
		messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(2, messageGroup.size());

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(2, messageGroup.size());
	}

	@Test
	@RedisAvailable
	public void testWithMessageHistory() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessagesToGroup(this.groupId, message);

		message = store.getMessageGroup(this.groupId).getMessages().iterator().next();

		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}

	@Test
	@RedisAvailable
	public void testRemoveNonExistingMessageFromTheGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		store.addMessagesToGroup(messageGroup.getGroupId(), new GenericMessage<>("1"));
		store.removeMessagesFromGroup(this.groupId, new GenericMessage<>("2"));
	}

	@Test
	@RedisAvailable
	public void testRemoveNonExistingMessageFromNonExistingTheGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		store.removeMessagesFromGroup(this.groupId, new GenericMessage<>("2"));
	}


	@Test
	@RedisAvailable
	public void testMultipleInstancesOfGroupStore() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store1 = new RedisMessageStore(jcf);

		RedisMessageStore store2 = new RedisMessageStore(jcf);

		store1.removeMessageGroup(this.groupId);

		Message<?> message = new GenericMessage<>("1");
		store1.addMessagesToGroup(this.groupId, message);
		MessageGroup messageGroup = store2.addMessageToGroup(this.groupId, new GenericMessage<>("2"));

		assertEquals(2, messageGroup.getMessages().size());

		RedisMessageStore store3 = new RedisMessageStore(jcf);

		store3.removeMessagesFromGroup(this.groupId, message);
		messageGroup = store3.getMessageGroup(this.groupId);

		assertEquals(1, messageGroup.getMessages().size());
	}

	@Test
	@RedisAvailable
	public void testIteratorOfMessageGroups() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store1 = new RedisMessageStore(jcf);
		RedisMessageStore store2 = new RedisMessageStore(jcf);

		store1.removeMessageGroup(this.groupId);
		UUID group2 = UUID.randomUUID();
		store1.removeMessageGroup(group2);
		UUID group3 = UUID.randomUUID();
		store1.removeMessageGroup(group3);

		store1.addMessagesToGroup(this.groupId, new GenericMessage<>("1"));
		store2.addMessagesToGroup(group2, new GenericMessage<>("2"));
		store1.addMessagesToGroup(group3, new GenericMessage<>("3"), new GenericMessage<>("3A"));

		Iterator<MessageGroup> messageGroups = store1.iterator();
		int counter = 0;
		while (messageGroups.hasNext()) {
			MessageGroup group = messageGroups.next();
			String groupId = (String) group.getGroupId();
			if (groupId.equals("1")) {
				assertEquals(1, group.getMessages().size());
			}
			else if (groupId.equals("2")) {
				assertEquals(1, group.getMessages().size());
			}
			else if (groupId.equals("3")) {
				assertEquals(2, group.getMessages().size());
			}
			counter++;
		}
		assertEquals(3, counter);

		store2.removeMessageGroup(group3);

		messageGroups = store1.iterator();
		counter = 0;
		while (messageGroups.hasNext()) {
			messageGroups.next();
			counter++;
		}
		assertEquals(2, counter);
	}

	@Test
	@RedisAvailable
	@Ignore
	public void testConcurrentModifications() throws Exception {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		final RedisMessageStore store1 = new RedisMessageStore(jcf);
		final RedisMessageStore store2 = new RedisMessageStore(jcf);

		store1.removeMessageGroup(this.groupId);

		final Message<?> message = new GenericMessage<>("1");

		ExecutorService executor = null;

		final List<Object> failures = new ArrayList<>();

		for (int i = 0; i < 100; i++) {
			executor = Executors.newCachedThreadPool();

			executor.execute(() -> {
				MessageGroup group = store1.addMessageToGroup(this.groupId, message);
				if (group.getMessages().size() != 1) {
					failures.add("ADD");
					throw new AssertionFailedError("Failed on ADD");
				}
			});
			executor.execute(() -> {
				store2.removeMessagesFromGroup(this.groupId, message);
				MessageGroup group = store2.getMessageGroup(this.groupId);
				if (group.getMessages().size() != 0) {
					failures.add("REMOVE");
					throw new AssertionFailedError("Failed on Remove");
				}
			});

			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			store2.removeMessagesFromGroup(1, message); // ensures that if ADD thread executed after REMOVE, the store is empty for the next cycle
		}
		assertTrue(failures.size() == 0);
	}

	@Test
	@RedisAvailable
	public void testWithAggregatorWithShutdown() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("redis-aggregator-config.xml", getClass());
		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m1 = MessageBuilder.withPayload("1")
				.setSequenceNumber(1)
				.setSequenceSize(3)
				.setCorrelationId(this.groupId)
				.build();

		Message<?> m2 = MessageBuilder.withPayload("2")
				.setSequenceNumber(2)
				.setSequenceSize(3)
				.setCorrelationId(this.groupId)
				.build();

		input.send(m1);
		assertNull(output.receive(10));
		input.send(m2);
		assertNull(output.receive(10));

		context.close();

		context = new ClassPathXmlApplicationContext("redis-aggregator-config.xml", getClass());
		input = context.getBean("inputChannel", MessageChannel.class);
		output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m3 = MessageBuilder.withPayload("3")
				.setSequenceNumber(3)
				.setSequenceSize(3)
				.setCorrelationId(this.groupId)
				.build();

		input.send(m3);
		assertNotNull(output.receive(10000));
		context.close();
	}

	@Test
	@RedisAvailable
	public void testAddAndRemoveMessagesFromMessageGroup() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore messageStore = new RedisMessageStore(jcf);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(this.groupId).build();
			messageStore.addMessagesToGroup(this.groupId, message);
			messages.add(message);
		}
		MessageGroup group = messageStore.getMessageGroup(this.groupId);
		assertEquals(25, group.size());
		messageStore.removeMessagesFromGroup(this.groupId, messages);
		group = messageStore.getMessageGroup(this.groupId);
		assertEquals(0, group.size());
		messageStore.removeMessageGroup(this.groupId);
	}

	@Test
	@RedisAvailable
	public void testJsonSerialization() {
		RedisConnectionFactory jcf = getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		ObjectMapper mapper = JacksonJsonUtils.messagingAwareMapper();

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		Message<?> genericMessage = new GenericMessage<>(new Date());
		Message<?> mutableMessage = new MutableMessage<>(UUID.randomUUID());
		Message<?> adviceMessage = new AdviceMessage<>("foo", genericMessage);
		ErrorMessage errorMessage = new ErrorMessage(new RuntimeException("test exception"), mutableMessage);

		store.addMessagesToGroup(this.groupId, genericMessage, mutableMessage, adviceMessage, errorMessage);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		assertEquals(4, messageGroup.size());
		List<Message<?>> messages = new ArrayList<>(messageGroup.getMessages());
		assertEquals(genericMessage, messages.get(0));
		assertEquals(mutableMessage, messages.get(1));
		assertEquals(adviceMessage, messages.get(2));
		Message<?> errorMessageResult = messages.get(3);
		assertEquals(errorMessage.getHeaders(), errorMessageResult.getHeaders());
		assertThat(errorMessageResult, instanceOf(ErrorMessage.class));
		assertEquals(errorMessage.getOriginalMessage(), ((ErrorMessage) errorMessageResult).getOriginalMessage());
		assertEquals(errorMessage.getPayload().getMessage(),
				((ErrorMessage) errorMessageResult).getPayload().getMessage());

		Message<Foo> fooMessage = new GenericMessage<>(new Foo("foo"));
		try {
			store.addMessageToGroup(this.groupId, fooMessage)
					.getMessages()
					.iterator()
					.next();
			fail("SerializationException expected");
		}
		catch (Exception e) {
			assertThat(e.getCause().getCause(), instanceOf(IllegalArgumentException.class));
			assertThat(e.getMessage(),
					containsString("The class with " +
							"org.springframework.integration.redis.store.RedisMessageGroupStoreTests$Foo and name of " +
							"org.springframework.integration.redis.store.RedisMessageGroupStoreTests$Foo " +
							"is not in the trusted packages:"));
		}

		mapper = JacksonJsonUtils.messagingAwareMapper(getClass().getPackage().getName());

		serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		store.removeMessageGroup(this.groupId);
		messageGroup = store.addMessageToGroup(this.groupId, fooMessage);
		assertEquals(1, messageGroup.size());
		assertEquals(fooMessage, messageGroup.getMessages().iterator().next());

		mapper = JacksonJsonUtils.messagingAwareMapper("*");

		serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		store.removeMessageGroup(this.groupId);
		messageGroup = store.addMessageToGroup(this.groupId, fooMessage);
		assertEquals(1, messageGroup.size());
		assertEquals(fooMessage, messageGroup.getMessages().iterator().next());
	}

	private static class Foo {

		private String foo;

		Foo() {
		}

		Foo(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Foo foo1 = (Foo) o;

			return this.foo != null ? this.foo.equals(foo1.foo) : foo1.foo == null;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.foo);
		}

	}

}
