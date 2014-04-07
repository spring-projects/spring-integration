/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jdbc.store.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Gunnar Hillert
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class HsqlTxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests {

	@Autowired
	private PollableChannel priorityChannel;

	@Test
	@Override
	public void test() throws InterruptedException {
		super.test();
	}

	@Test
	@Override
	public void testInt2993IdCacheConcurrency() throws InterruptedException, ExecutionException {
		super.testInt2993IdCacheConcurrency();
	}

	@Test
	@Override
	public void testInt3181ConcurrentPolling() throws InterruptedException {
		super.testInt3181ConcurrentPolling();
	}

	@Test
	public void testPriorityChannel() throws Exception {
		Message<String> message = MessageBuilder.withPayload("1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 1).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("-1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, -1).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("3").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("0").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 0).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("2").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 2).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("none").build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("31").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		priorityChannel.send(message);

		Message<?> receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("3", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("31", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("2", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("1", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("0", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("-1", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("none", receive.getPayload());
	}

}
