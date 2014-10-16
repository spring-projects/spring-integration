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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentReceiverTests {

	@Test
	public void testInvalidCtorArgs() {
		try {
			new IdempotentReceiver(null);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalArgumentException.class));
			assertEquals("'idempotentKeyStrategy' can't be null", e.getMessage());
		}

	}

	@Test
	public void testIdempotentKeyStrategy() {
		MetadataStore store = new SimpleMetadataStore();
		ExpressionIdempotentKeyStrategy idempotentKeyStrategy = new ExpressionIdempotentKeyStrategy("payload");
		idempotentKeyStrategy.setBeanFactory(Mockito.mock(BeanFactory.class));
		IdempotentReceiver idempotentReceiver = new IdempotentReceiver(idempotentKeyStrategy, store);
		QueueChannel outputChannel = new QueueChannel();
		idempotentReceiver.setOutputChannel(outputChannel);
		idempotentReceiver.setThrowExceptionOnRejection(true);

		idempotentReceiver.handleMessage(new GenericMessage<String>("foo"));
		assertNotNull(outputChannel.receive(10000));
		assertEquals(1, TestUtils.getPropertyValue(store, "metadata", Map.class).size());
		assertNotNull(store.get("foo"));

		try {
			idempotentReceiver.handleMessage(new GenericMessage<String>("foo"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageRejectedException.class));
		}

		idempotentReceiver.setFilter(false);
		idempotentReceiver.handleMessage(new GenericMessage<String>("foo"));
		Message<?> duplicate = outputChannel.receive(10000);
		assertNotNull(duplicate);
		assertTrue(duplicate.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(store, "metadata", Map.class).size());
	}

}
