/*
 * Copyright 2007-present the original author or authors.
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.util.Address;
import org.springframework.integration.redis.util.Person;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Yordan Tsintsov
 *
 */
class RedisMessageStoreTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	private static final String DEFAULT_PERSON_ADDRESS = "1600 Pennsylvania Av, Washington, DC";

	private static final String DEFAULT_PERSON_NAME = "Barak Obama";

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@BeforeEach
	@AfterEach
	public void setUpTearDown() {
		StringRedisTemplate template = RedisContainerTest.createStringRedisTemplate(redisConnectionFactory);
		template.delete(template.keys("*MESSAGE_*"));
	}

	@Test
	void testGetNonExistingMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Message<?> message = store.getMessage(UUID.randomUUID());
		assertThat(message).isNull();
	}

	@Test
	void testGetMessageCountWhenEmpty() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		assertThat(store.getMessageCount()).isZero();
	}

	@Test
	void testAddStringMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Message<String> stringMessage = new GenericMessage<>("Hello Redis");
		Message<String> storedMessage = store.addMessage(stringMessage);
		assertThat(storedMessage).isSameAs(stringMessage);
	}

	@Test
	void testAddSerializableObjectMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Address address = new Address();
		address.setAddress("1600 Pennsylvania Av, Washington, DC");
		Person person = new Person(address, "Barak Obama");

		Message<Person> objectMessage = new GenericMessage<>(person);
		Message<Person> storedMessage = store.addMessage(objectMessage);
		assertThat(storedMessage).isSameAs(objectMessage);
	}

	@Test
	void testAddNonSerializableObjectMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);

		Message<Foo> objectMessage = new GenericMessage<>(new Foo());

		assertThatThrownBy(() -> store.addMessage(objectMessage)).isInstanceOf(IllegalArgumentException.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void testAddAndGetStringMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Message<String> stringMessage = new GenericMessage<>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.getMessage(stringMessage.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo("Hello Redis");
	}

	@SuppressWarnings("unchecked")
	@Test
	void testAddAndGetWithPrefix() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory, "foo");
		Message<String> stringMessage = new GenericMessage<>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.getMessage(stringMessage.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo("Hello Redis");

		StringRedisTemplate template = RedisContainerTest.createStringRedisTemplate(redisConnectionFactory);
		BoundValueOperations<String, String> ops =
				template.boundValueOps("foo" + "MESSAGE_" + stringMessage.getHeaders().getId());
		assertThat(ops.get()).isNotNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testAddAndRemoveStringMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Message<String> stringMessage = new GenericMessage<>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.removeMessage(stringMessage.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo("Hello Redis");
		assertThat(store.getMessage(stringMessage.getHeaders().getId())).isNull();
	}

	@Test
	void testRemoveNonExistingMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Message<?> removedMessage = store.removeMessage(UUID.randomUUID());
		assertThat(removedMessage).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testAddAndRemoveSerializableObjectMessage() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);
		Address address = new Address();
		address.setAddress(DEFAULT_PERSON_ADDRESS);
		Person person = new Person(address, DEFAULT_PERSON_NAME);

		Message<Person> objectMessage = new GenericMessage<>(person);
		store.addMessage(objectMessage);
		Message<Person> retrievedMessage = (Message<Person>) store.removeMessage(objectMessage.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload().getName()).isEqualTo(DEFAULT_PERSON_NAME);
		assertThat(retrievedMessage.getPayload().getAddress().getAddress()).isEqualTo(DEFAULT_PERSON_ADDRESS);
		assertThat(store.getMessage(objectMessage.getHeaders().getId())).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRemoveMultipleMessagesSequentially() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);

		Message<String> message1 = new GenericMessage<>("Message 1");
		Message<String> message2 = new GenericMessage<>("Message 2");
		Message<String> message3 = new GenericMessage<>("Message 3");

		store.addMessage(message1);
		store.addMessage(message2);
		store.addMessage(message3);

		assertThat(store.getMessageCount()).isEqualTo(3);

		Message<String> removed1 = (Message<String>) store.removeMessage(message1.getHeaders().getId());
		assertThat(removed1).isNotNull();
		assertThat(removed1.getPayload()).isEqualTo("Message 1");
		assertThat(store.getMessageCount()).isEqualTo(2);

		Message<String> removed2 = (Message<String>) store.removeMessage(message2.getHeaders().getId());
		assertThat(removed2).isNotNull();
		assertThat(removed2.getPayload()).isEqualTo("Message 2");
		assertThat(store.getMessageCount()).isEqualTo(1);

		Message<String> removed3 = (Message<String>) store.removeMessage(message3.getHeaders().getId());
		assertThat(removed3).isNotNull();
		assertThat(removed3.getPayload()).isEqualTo("Message 3");
		assertThat(store.getMessageCount()).isZero();
	}

	@Test
	void testWithMessageHistory() {
		RedisMessageStore store = new RedisMessageStore(redisConnectionFactory);

		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessage(message);
		message = store.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory.size()).isEqualTo(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory)
				.containsEntry("name", "fooChannel")
				.containsEntry("type", "channel");
	}

	@Test
	void testAddAndRemoveMessagesFromMessageGroup() {
		RedisMessageStore messageStore = new RedisMessageStore(redisConnectionFactory);
		String groupId = "X";
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messageStore.addMessagesToGroup(groupId, message);
			messages.add(message);
		}
		messageStore.removeMessagesFromGroup(groupId, messages);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isZero();
		messageStore.removeMessageGroup("X");
	}

	public static class Foo {

	}

}
