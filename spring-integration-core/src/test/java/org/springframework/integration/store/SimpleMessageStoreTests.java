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

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Gary Russell
 * @author Ryan Barker
 * @author Artem Bilan
 */
public class SimpleMessageStoreTests {

	@Test
	@SuppressWarnings("unchecked")
	public void shouldRetainMessage() {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessage(testMessage1);
		assertThat(store.getMessage(testMessage1.getHeaders().getId())).isEqualTo(testMessage1);
	}

	@Test
	public void shouldNotHoldMoreThanCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessage(testMessage1);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessage(testMessage2));
	}

	@Test
	public void shouldReleaseCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessage(testMessage1);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessage(testMessage2))
				.withMessageContaining("was out of capacity (1)");

		store.removeMessage(testMessage2.getHeaders().getId());

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessage(testMessage2))
				.withMessageContaining("was out of capacity (1)");

		store.removeMessage(testMessage1.getHeaders().getId());
		store.addMessage(testMessage2);

	}

	@Test
	public void shouldWaitIfCapacity() throws InterruptedException {
		final SimpleMessageStore store2 = new SimpleMessageStore(1, 1, 1000);
		final Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		final Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();

		store2.addMessage(testMessage1);

		final CountDownLatch message2Latch = new CountDownLatch(1);

		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			store2.addMessage(testMessage2);
			message2Latch.countDown();
		});
		// Simulate a blocked consumer
		Thread.sleep(10);
		Message<?> t1 = store2.removeMessage(testMessage1.getHeaders().getId());
		assertThat(t1).isEqualTo(testMessage1);

		assertThat(message2Latch.await(10, TimeUnit.SECONDS)).isTrue();
		Message<?> t2 = store2.getMessage(testMessage2.getHeaders().getId());
		assertThat(t2).isEqualTo(testMessage2);
		exec.shutdownNow();
	}

	@Test
	public void shouldTimeoutAfterWaitIfCapacity() {
		SimpleMessageStore store2 = new SimpleMessageStore(1, 1, 10);
		store2.addMessage(new GenericMessage<Object>("foo"));
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store2.addMessage(new GenericMessage<Object>("foo")));
	}

	@Test
	public void shouldNotHoldMoreThanGroupCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(0, 1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessageToGroup("foo", testMessage1);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessageToGroup("foo", testMessage2));
	}

	@Test
	public void shouldWaitIfGroupCapacity() throws InterruptedException {
		final SimpleMessageStore store2 = new SimpleMessageStore(1, 1, 1000);
		final Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		final Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();

		store2.addMessageToGroup("foo", testMessage1);

		final CountDownLatch message2Latch = new CountDownLatch(1);

		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			store2.addMessageToGroup("foo", testMessage2);
			message2Latch.countDown();
		});
		// Simulate a blocked consumer
		Thread.sleep(10);
		store2.removeMessagesFromGroup("foo", testMessage1);

		assertThat(message2Latch.await(10, TimeUnit.SECONDS)).isTrue();
		MessageGroup messageGroup = store2.getMessageGroup("foo");
		assertThat(messageGroup.getMessages()).contains(testMessage2);
		exec.shutdownNow();
	}

	@Test
	public void shouldTimeoutAfterWaitIfGroupCapacity() {
		SimpleMessageStore store2 = new SimpleMessageStore(1, 1, 1);
		store2.addMessageToGroup("foo", MessageBuilder.withPayload("foo").build());

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store2.addMessageToGroup("foo", MessageBuilder.withPayload("bar").build()));
	}

	@Test
	public void shouldHoldCapacityExactly() {
		SimpleMessageStore store = new SimpleMessageStore(2);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessage(testMessage1);
		store.addMessage(testMessage2);
	}

	@Test
	public void shouldReleaseGroupCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(0, 1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessageToGroup("foo", testMessage1);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessageToGroup("foo", testMessage2))
				.withMessageContaining("was out of capacity (1) for group 'foo'");

		store.removeMessagesFromGroup("foo", testMessage2);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> store.addMessageToGroup("foo", testMessage2))
				.withMessageContaining("was out of capacity (1) for group 'foo'");

		store.removeMessagesFromGroup("foo", testMessage1);
		store.addMessageToGroup("foo", testMessage2);
	}

	@Test
	public void shouldListByCorrelation() {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertThat(store.getMessageGroup("bar").size()).isEqualTo(1);
	}

	@Test
	public void shouldRemoveFromGroup() {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		Message<?> testMessage2 = store.getMessageGroup("bar").getOne();
		store.removeMessagesFromGroup("bar", testMessage2);
		MessageGroup group = store.getMessageGroup("bar");
		assertThat(group.size()).isEqualTo(0);
		assertThat(store.getMessageGroup("bar").size()).isEqualTo(0);
	}

	@Test
	public void testRepeatedAddAndRemoveGroup() {
		SimpleMessageStore store = new SimpleMessageStore(10, 10);
		for (int i = 0; i < 10; i++) {
			store.addMessageToGroup("bar", MessageBuilder.withPayload("foo").build());
			store.addMessageToGroup("bar", MessageBuilder.withPayload("foo").build());
			store.removeMessageGroup("bar");
			assertThat(store.getMessageGroup("bar").size()).isEqualTo(0);
			assertThat(store.getMessageGroupCount()).isEqualTo(0);
		}
	}

	@Test
	public void shouldCopyMessageGroup() {
		SimpleMessageStore store = new SimpleMessageStore();
		store.setCopyOnGet(true);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertThat(store.getMessageGroup("bar")).isNotSameAs(store.getMessageGroup("bar"));
	}

	@Test
	public void shouldRegisterCallbacks() {
		SimpleMessageStore store = new SimpleMessageStore();
		store.setExpiryCallbacks(List.of((messageGroupStore, group) -> {
		}));
		assertThat(((Collection<?>) ReflectionTestUtils.getField(store, "expiryCallbacks"))).hasSize(1);
	}

	@Test
	public void shouldExpireMessageGroup() {

		SimpleMessageStore store = new SimpleMessageStore();
		final List<String> list = new ArrayList<>();
		store.registerMessageGroupExpiryCallback((messageGroupStore, group) -> {
			list.add(group.getOne().getPayload().toString());
			messageGroupStore.removeMessageGroup(group.getGroupId());
		});

		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertThat(store.getMessageGroup("bar").size()).isEqualTo(1);

		store.expireMessageGroups(-10000);
		assertThat(list.toString()).isEqualTo("[foo]");
		assertThat(store.getMessageGroup("bar").size()).isEqualTo(0);

	}

	@Test
	public void testAddAndRemoveMessagesFromMessageGroup() {
		SimpleMessageStore messageStore = new SimpleMessageStore();
		String groupId = "X";
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messageStore.addMessageToGroup(groupId, message);
			messages.add(message);
		}
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(25);
		messageStore.removeMessagesFromGroup(groupId, messages);
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

}
