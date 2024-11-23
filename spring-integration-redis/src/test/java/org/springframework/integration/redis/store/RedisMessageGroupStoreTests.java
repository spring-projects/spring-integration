/*
 * Copyright 2007-2025 the original author or authors.
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

package org.springframework.integration.redis.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 * @author Youbin Wu
 */
class RedisMessageGroupStoreTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	private final UUID groupId = UUID.randomUUID();

	RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);

	@BeforeEach
	@AfterEach
	void setUpTearDown() {
		StringRedisTemplate template = RedisContainerTest.createStringRedisTemplate(redisConnectionFactory);
		template.delete(template.keys("MESSAGE_*"));
		template.delete(template.keys("GROUP_OF_MESSAGES_*"));
	}

	@Test
	void testNonExistingEmptyMessageGroup() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup).isInstanceOf(SimpleMessageGroup.class);
		assertThat(messageGroup.size()).isZero();
	}

	@Test
	void testMessageGroupUpdatedDateChangesWithEachAddedMessage() throws Exception {
		Message<?> message = new GenericMessage<>("Hello");
		MessageGroup messageGroup = store.addMessageToGroup(this.groupId, message);
		assertThat(messageGroup.size()).isEqualTo(1);
		long createdTimestamp = messageGroup.getTimestamp();
		long updatedTimestamp = messageGroup.getLastModified();
		assertThat(updatedTimestamp).isEqualTo(createdTimestamp);
		Thread.sleep(10);
		message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(this.groupId, message);
		createdTimestamp = messageGroup.getTimestamp();
		updatedTimestamp = messageGroup.getLastModified();
		assertThat(updatedTimestamp > createdTimestamp).isTrue();

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(redisConnectionFactory);

		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.size()).isEqualTo(2);
	}

	@Test
	void testMessageGroupWithAddedMessage() {
		Message<?> message = new GenericMessage<>("Hello");
		MessageGroup messageGroup = store.addMessageToGroup(this.groupId, message);
		assertThat(messageGroup.size()).isEqualTo(1);

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(redisConnectionFactory);

		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.size()).isEqualTo(1);
	}

	@Test
	void testRemoveMessageGroup() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		assertThat(messageGroup.size()).isEqualTo(1);

		store.removeMessageGroup(this.groupId);
		MessageGroup messageGroupA = store.getMessageGroup(this.groupId);
		assertThat(messageGroupA).isNotSameAs(messageGroup);
		//		assertEquals(0, messageGroupA.getMarked().size());
		assertThat(messageGroupA.getMessages().size()).isZero();
		assertThat(messageGroupA.size()).isZero();

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(redisConnectionFactory);

		messageGroup = store.getMessageGroup(this.groupId);

		assertThat(messageGroup.getMessages().size()).isZero();
		assertThat(messageGroup.size()).isZero();
	}

	@Test
	void testCompleteMessageGroup() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.isComplete()).isTrue();
	}

	@Test
	void testLastReleasedSequenceNumber() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.getLastReleasedMessageSequenceNumber()).isEqualTo(5);
	}

	@Test
	void testRemoveMessageFromTheGroup() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		Message<?> message = new GenericMessage<>("2");
		store.addMessagesToGroup(messageGroup.getGroupId(), new GenericMessage<>("1"), message);
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), new GenericMessage<>("3"));
		assertThat(messageGroup.size()).isEqualTo(3);

		store.removeMessagesFromGroup(this.groupId, message);
		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.size()).isEqualTo(2);

		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(redisConnectionFactory);

		messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.size()).isEqualTo(2);
	}

	@Test
	void testWithMessageHistory() {
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
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory.size()).isEqualTo(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory)
				.containsEntry("name", "fooChannel")
				.containsEntry("type", "channel");
	}

	@Test
	void testRemoveNonExistingMessageFromTheGroup() {
		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		store.addMessagesToGroup(messageGroup.getGroupId(), new GenericMessage<>("1"));
		assertThatNoException()
				.isThrownBy(() -> store.removeMessagesFromGroup(this.groupId, new GenericMessage<>("2")));
	}

	@Test
	void testRemoveNonExistingMessageFromNonExistingTheGroup() {
		assertThatNoException()
				.isThrownBy(() -> store.removeMessagesFromGroup(this.groupId, new GenericMessage<>("2")));
	}

	@Test
	void testMultipleInstancesOfGroupStore() {
		RedisMessageStore store1 = new RedisMessageStore(redisConnectionFactory);

		RedisMessageStore store2 = new RedisMessageStore(redisConnectionFactory);

		store1.removeMessageGroup(this.groupId);

		Message<?> message = new GenericMessage<>("1");
		store1.addMessagesToGroup(this.groupId, message);
		MessageGroup messageGroup = store2.addMessageToGroup(this.groupId, new GenericMessage<>("2"));

		assertThat(messageGroup.getMessages().size()).isEqualTo(2);

		RedisMessageStore store3 = new RedisMessageStore(redisConnectionFactory);

		store3.removeMessagesFromGroup(this.groupId, message);
		messageGroup = store3.getMessageGroup(this.groupId);

		assertThat(messageGroup.getMessages().size()).isEqualTo(1);
	}

	@Test
	void testIteratorOfMessageGroups() {
		RedisMessageStore store1 = new RedisMessageStore(redisConnectionFactory);
		RedisMessageStore store2 = new RedisMessageStore(redisConnectionFactory);

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
			switch (groupId) {
				case "1", "2" -> assertThat(group.getMessages().size()).isEqualTo(1);
				case "3" -> assertThat(group.getMessages().size()).isEqualTo(2);
			}
			counter++;
		}
		assertThat(counter).isEqualTo(3);

		store2.removeMessageGroup(group3);

		messageGroups = store1.iterator();
		counter = 0;
		while (messageGroups.hasNext()) {
			messageGroups.next();
			counter++;
		}
		assertThat(counter).isEqualTo(2);
	}

	@Test
	@Disabled
	void testConcurrentModifications() throws Exception {
		final RedisMessageStore store1 = new RedisMessageStore(redisConnectionFactory);
		final RedisMessageStore store2 = new RedisMessageStore(redisConnectionFactory);

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
				if (!group.getMessages().isEmpty()) {
					failures.add("REMOVE");
					throw new AssertionFailedError("Failed on Remove");
				}
			});

			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			store2.removeMessagesFromGroup(1, message); // ensures that if ADD thread executed after REMOVE, the store is empty for the next cycle
		}
		assertThat(failures).isEmpty();
	}

	@Test
	void testWithAggregatorWithShutdown() {
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
		assertThat(output.receive(10)).isNull();
		input.send(m2);
		assertThat(output.receive(10)).isNull();

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
		assertThat(output.receive(10000)).isNotNull();
		context.close();
	}

	@Test
	void testAddAndRemoveMessagesFromMessageGroup() {
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(this.groupId).build();
			store.addMessagesToGroup(this.groupId, message);
			messages.add(message);
		}
		MessageGroup group = store.getMessageGroup(this.groupId);
		assertThat(group.size()).isEqualTo(25);
		store.removeMessagesFromGroup(this.groupId, messages);
		group = store.getMessageGroup(this.groupId);
		assertThat(group.size()).isZero();
		store.removeMessageGroup(this.groupId);
	}

	@Test
	void testJsonSerialization() {
		ObjectMapper mapper = JacksonJsonUtils.messagingAwareMapper();

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		Message<?> genericMessage = new GenericMessage<>(new Date());
		NullChannel testComponent = new NullChannel();
		testComponent.setBeanName("testChannel");
		genericMessage = MessageHistory.write(genericMessage, testComponent);
		Message<?> mutableMessage = new MutableMessage<>(UUID.randomUUID());
		Message<?> adviceMessage = new AdviceMessage<>("foo", genericMessage);
		ErrorMessage errorMessage = new ErrorMessage(new RuntimeException("test exception"), mutableMessage);
		var delayedMessageWrapperConstructor =
				BeanUtils.getResolvableConstructor(DelayHandler.DelayedMessageWrapper.class);
		Message<?> delayMessage = new GenericMessage<>(
				BeanUtils.instantiateClass(delayedMessageWrapperConstructor, genericMessage,
						System.currentTimeMillis()));

		store.addMessagesToGroup(this.groupId,
				genericMessage, mutableMessage, adviceMessage, errorMessage, delayMessage);

		MessageGroup messageGroup = store.getMessageGroup(this.groupId);
		assertThat(messageGroup.size()).isEqualTo(5);
		List<Message<?>> messages = new ArrayList<>(messageGroup.getMessages());
		assertThat(messages.get(0)).isEqualTo(genericMessage);
		assertThat(messages.get(0).getHeaders()).containsKeys(MessageHistory.HEADER_NAME);
		assertThat(messages.get(1)).isEqualTo(mutableMessage);
		assertThat(messages.get(2)).isEqualTo(adviceMessage);
		Message<?> errorMessageResult = messages.get(3);
		assertThat(errorMessageResult.getHeaders()).isEqualTo(errorMessage.getHeaders());
		assertThat(errorMessageResult).isInstanceOf(ErrorMessage.class);
		assertThat(((ErrorMessage) errorMessageResult).getOriginalMessage())
				.isEqualTo(errorMessage.getOriginalMessage());
		assertThat(((ErrorMessage) errorMessageResult).getPayload().getMessage())
				.isEqualTo(errorMessage.getPayload().getMessage());
		assertThat(messages.get(4)).isEqualTo(delayMessage);

		Message<Foo> fooMessage = new GenericMessage<>(new Foo("foo"));

		assertThatExceptionOfType(SerializationException.class)
				.isThrownBy(() ->
						store.addMessageToGroup(this.groupId, fooMessage)
								.getMessages()
								.iterator()
								.next())
				.withRootCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("The class with " +
						"org.springframework.integration.redis.store.RedisMessageGroupStoreTests$Foo and name of " +
						"org.springframework.integration.redis.store.RedisMessageGroupStoreTests$Foo " +
						"is not in the trusted packages:");

		mapper = JacksonJsonUtils.messagingAwareMapper(getClass().getPackage().getName());

		serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		store.removeMessageGroup(this.groupId);
		messageGroup = store.addMessageToGroup(this.groupId, fooMessage);
		assertThat(messageGroup.size()).isEqualTo(1);
		assertThat(messageGroup.getMessages().iterator().next()).isEqualTo(fooMessage);

		mapper = JacksonJsonUtils.messagingAwareMapper("*");

		serializer = new GenericJackson2JsonRedisSerializer(mapper);
		store.setValueSerializer(serializer);

		store.removeMessageGroup(this.groupId);
		messageGroup = store.addMessageToGroup(this.groupId, fooMessage);
		assertThat(messageGroup.size()).isEqualTo(1);
		assertThat(messageGroup.getMessages().iterator().next()).isEqualTo(fooMessage);
	}

	@Test
	public void sameMessageInTwoGroupsNotRemovedByFirstGroup() {
		GenericMessage<String> testMessage = new GenericMessage<>("test data");

		store.addMessageToGroup("1", testMessage);
		store.addMessageToGroup("2", testMessage);

		store.removeMessageGroup("1");

		assertThat(store.getMessageCount()).isEqualTo(1);

		store.removeMessageGroup("2");

		assertThat(store.getMessageCount()).isEqualTo(0);
	}

	@Test
	public void removeMessagesFromGroupDontRemoveSameMessageInOtherGroup() {
		GenericMessage<String> testMessage = new GenericMessage<>("test data");

		store.addMessageToGroup("1", testMessage);
		store.addMessageToGroup("2", testMessage);

		store.removeMessagesFromGroup("1", testMessage);

		assertThat(store.getMessageCount()).isEqualTo(1);
		assertThat(store.messageGroupSize("1")).isEqualTo(0);
		assertThat(store.messageGroupSize("2")).isEqualTo(1);
	}

	@Test
	public void testMessageGroupCondition() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		store.addMessagesToGroup(groupId, message);
		store.setGroupCondition(groupId, "testCondition");
		assertThat(store.getMessageGroup(groupId).getCondition()).isEqualTo("testCondition");
	}

	private record Foo(String foo) {

	}

}
