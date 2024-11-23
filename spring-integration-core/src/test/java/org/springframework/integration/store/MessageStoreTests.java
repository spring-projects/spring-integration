/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @author Youbin Wu
 */
public class MessageStoreTests {

	@Test
	public void shouldRegisterCallbacks() {
		TestMessageStore store = new TestMessageStore();
		store.setExpiryCallbacks(Collections.singletonList((messageGroupStore, group) -> {
		}));
		assertThat(((Collection<?>) ReflectionTestUtils.getField(store, "expiryCallbacks")).size()).isEqualTo(1);
	}

	@Test
	public void shouldExpireMessageGroup() {
		TestMessageStore store = new TestMessageStore();
		final List<String> list = new ArrayList<>();
		store.registerMessageGroupExpiryCallback((messageGroupStore, group) -> {
			list.add(group.getOne().getPayload().toString());
			messageGroupStore.removeMessageGroup(group.getGroupId());
		});

		store.expireMessageGroups(-10000);
		assertThat(list.toString()).isEqualTo("[foo]");
		assertThat(store.getMessageGroup("bar").size()).isEqualTo(0);

	}

	@Test
	public void testGroupCount() {
		TestMessageStore store = new TestMessageStore();
		assertThat(store.getMessageGroupCount()).isEqualTo(1);
	}

	@Test
	public void testGroupSizes() {
		TestMessageStore store = new TestMessageStore();
		assertThat(store.getMessageCountForAllMessageGroups()).isEqualTo(1);
	}

	private static class TestMessageStore extends SimpleMessageStore {

		MessageGroup testMessages =
				new SimpleMessageGroup(Collections.singletonList(new GenericMessage<String>("foo")), "bar");

		private boolean removed = false;

		TestMessageStore() {
			super();
		}

		@Override
		public Iterator<MessageGroup> iterator() {
			return Collections.singletonList(testMessages).iterator();
		}

		@Override
		protected void doAddMessagesToGroup(Object groupId, Message<?>... messages) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageGroup getMessageGroup(Object correlationKey) {
			return removed ? new SimpleMessageGroup(correlationKey) : testMessages;
		}

		@Override
		protected void doRemoveMessagesFromGroup(Object key, Collection<Message<?>> messages) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void doRemoveMessageGroup(Object correlationKey) {
			if (correlationKey.equals(testMessages.getGroupId())) {
				removed = true;
			}
		}

		@Override
		protected void doSetLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void doCompleteGroup(Object groupId) {

			throw new UnsupportedOperationException();
		}

		@Override
		protected Message<?> doPollMessageFromGroup(Object groupId) {
			return null;
		}

		@Override
		public int messageGroupSize(Object groupId) {
			return 0;
		}

	}

}
