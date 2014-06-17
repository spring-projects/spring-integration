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

package org.springframework.integration.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Gary Russell
 */
public class SimpleMessageStoreTests {

	@Test
	@SuppressWarnings("unchecked")
	public void shouldRetainMessage() {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessage(testMessage1);
		assertThat((Message<String>) store.getMessage(testMessage1.getHeaders().getId()), is(testMessage1));
	}

	@Test(expected = MessagingException.class)
	public void shouldNotHoldMoreThanCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessage(testMessage1);
		store.addMessage(testMessage2);
	}

	@Test(expected = MessagingException.class)
	public void shouldNotHoldMoreThanGroupCapacity() {
		SimpleMessageStore store = new SimpleMessageStore(0, 1);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		Message<String> testMessage2 = MessageBuilder.withPayload("bar").build();
		store.addMessageToGroup("foo", testMessage1);
		store.addMessageToGroup("foo", testMessage2);
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
	public void shouldListByCorrelation() throws Exception {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertEquals(1, store.getMessageGroup("bar").size());
	}

	@Test
	public void shouldRemoveFromGroup() throws Exception {
		SimpleMessageStore store = new SimpleMessageStore();
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		Message<?> testMessage2 = store.getMessageGroup("bar").getOne();
		MessageGroup group = store.removeMessageFromGroup("bar", testMessage2);
		assertEquals(0, group.size());
		assertEquals(0, store.getMessageGroup("bar").size());
	}

	@Test
	public void testRepeatedAddAndRemoveGroup() throws Exception {
		SimpleMessageStore store = new SimpleMessageStore(10, 10);
		for (int i = 0; i < 10; i++) {
			store.addMessageToGroup("bar", MessageBuilder.withPayload("foo").build());
			store.addMessageToGroup("bar", MessageBuilder.withPayload("foo").build());
			store.removeMessageGroup("bar");
			assertEquals(0, store.getMessageGroup("bar").size());
			assertEquals(0, store.getMessageGroupCount());
		}
	}

	@Test
	public void shouldCopyMessageGroup() throws Exception {
		SimpleMessageStore store = new SimpleMessageStore();
		store.setCopyOnGet(true);
		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertNotSame(store.getMessageGroup("bar"), store.getMessageGroup("bar"));
	}

	@Test
	public void shouldRegisterCallbacks() throws Exception {
		SimpleMessageStore store = new SimpleMessageStore();
		store.setExpiryCallbacks(Arrays.<MessageGroupCallback> asList(new MessageGroupStore.MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
			}
		}));
		assertEquals(1, ((Collection<?>) ReflectionTestUtils.getField(store, "expiryCallbacks")).size());
	}

	@Test
	public void shouldExpireMessageGroup() throws Exception {

		SimpleMessageStore store = new SimpleMessageStore();
		final List<String> list = new ArrayList<String>();
		store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				list.add(group.getOne().getPayload().toString());
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});

		Message<String> testMessage1 = MessageBuilder.withPayload("foo").build();
		store.addMessageToGroup("bar", testMessage1);
		assertEquals(1, store.getMessageGroup("bar").size());

		store.expireMessageGroups(-10000);
		assertEquals("[foo]", list.toString());
		assertEquals(0, store.getMessageGroup("bar").size());

	}

}
