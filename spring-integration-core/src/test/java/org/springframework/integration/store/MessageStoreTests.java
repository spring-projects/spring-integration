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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class MessageStoreTests {

	@Test
	public void shouldRegisterCallbacks() throws Exception {
		TestMessageStore store = new TestMessageStore();
		store.setExpiryCallbacks(Arrays.<MessageGroupCallback> asList(new MessageGroupStore.MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
			}
		}));
		assertEquals(1, ((Collection<?>) ReflectionTestUtils.getField(store, "expiryCallbacks")).size());
	}

	@Test
	public void shouldExpireMessageGroup() throws Exception {

		TestMessageStore store = new TestMessageStore();
		final List<String> list = new ArrayList<String>();
		store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				list.add(group.getOne().getPayload().toString());
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});

		store.expireMessageGroups(-10000);
		assertEquals("[foo]", list.toString());
		assertEquals(0, store.getMessageGroup("bar").size());

	}

	@Test
	public void testGroupCount() throws Exception {
		TestMessageStore store = new TestMessageStore();
		assertEquals(1, store.getMessageGroupCount());
	}

	@Test
	public void testGroupSizes() throws Exception {
		TestMessageStore store = new TestMessageStore();
		assertEquals(1, store.getMessageCountForAllMessageGroups());
	}

	private static class TestMessageStore extends AbstractMessageGroupStore {

		@SuppressWarnings("unchecked")
		MessageGroup testMessages = new SimpleMessageGroup(Arrays.asList(new GenericMessage<String>("foo")), "bar");

		private boolean removed = false;


		public Iterator<MessageGroup> iterator() {
			return Arrays.asList(testMessages).iterator();
		}

		public MessageGroup addMessageToGroup(Object correlationKey, Message<?> message) {
			throw new UnsupportedOperationException();
		}

		public MessageGroup getMessageGroup(Object correlationKey) {
			return removed ? new SimpleMessageGroup(correlationKey) : testMessages;
		}

		public MessageGroup removeMessageFromGroup(Object key, Message<?> messageToRemove) {
			throw new UnsupportedOperationException();
		}

		public void removeMessageGroup(Object correlationKey) {
			if (correlationKey.equals(testMessages.getGroupId())) {
				removed = true;
			}
		}

		public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
			throw new UnsupportedOperationException();
		}

		public void completeGroup(Object groupId) {

			throw new UnsupportedOperationException();
		}

		public Message<?> pollMessageFromGroup(Object groupId) {
			return null;
		}

		public int messageGroupSize(Object groupId) {
			return 0;
		}

	}

}
