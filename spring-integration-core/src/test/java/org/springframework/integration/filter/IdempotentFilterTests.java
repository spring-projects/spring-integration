/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.filter;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentFilterTests {

	@Test
	public void testInvalidCtorArgs() {
		try {
			new IdempotentFilter("foo", (IdempotentKeyStrategy) null);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertEquals("One of 'idempotentSelector' or 'idempotentKeyStrategy' must be provided", e.getMessage());
		}

	}

	@Test
	public void testIdempotentKeyStrategy() {
		MessageGroupStore store = new SimpleMessageStore(1);
		ExpressionIdempotentKeyStrategy idempotentKeyStrategy = new ExpressionIdempotentKeyStrategy("payload");
		idempotentKeyStrategy.setBeanFactory(Mockito.mock(BeanFactory.class));
		IdempotentFilter idempotentFilter = new IdempotentFilter("foo", idempotentKeyStrategy, store);
		QueueChannel outputChannel = new QueueChannel();
		idempotentFilter.setOutputChannel(outputChannel);
		idempotentFilter.setThrowExceptionOnRejection(true);

		idempotentFilter.handleMessage(new GenericMessage<String>("foo"));
		assertNotNull(outputChannel.receive(10000));
		assertEquals(1, store.getMessageCountForAllMessageGroups());
		MessageGroup messageGroup = store.getMessageGroup("foo");
		Collection<Message<?>> messages = messageGroup.getMessages();
		assertFalse(messages.isEmpty());

		try {
			idempotentFilter.handleMessage(new GenericMessage<String>("foo"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageRejectedException.class));
		}

		idempotentFilter.setSkipDuplicate(false);
		idempotentFilter.handleMessage(new GenericMessage<String>("foo"));
		Message<?> duplicate = outputChannel.receive(10000);
		assertNotNull(duplicate);
		assertTrue(duplicate.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class));
		assertEquals(1, store.getMessageCountForAllMessageGroups());
	}

	@Test
	public void testIdempotentSelector() {
		final AtomicBoolean accepted = new AtomicBoolean();
		IdempotentFilter idempotentFilter = new IdempotentFilter("foo", new IdempotentSelector() {
			@Override
			public boolean accept(Collection<Message<?>> messages, Message<?> message) {
				return !accepted.getAndSet(true);
			}
		});
		QueueChannel outputChannel = new QueueChannel();
		idempotentFilter.setOutputChannel(outputChannel);
		idempotentFilter.setThrowExceptionOnRejection(true);

		idempotentFilter.handleMessage(new GenericMessage<String>("foo"));
		assertNotNull(outputChannel.receive(10000));

		try {
			idempotentFilter.handleMessage(new GenericMessage<String>("bar"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageRejectedException.class));
		}
	}

}
