/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionSynchronizationQueueChannelTests {

	@Autowired
	private PollableChannel queueChannel;

	@Autowired
	private PollableChannel good;

	@Autowired
	private Service service;

	@Autowired
	private PollableChannel queueChannel2;

	@Test
	public void testCommit() throws Exception {
		service.latch = new CountDownLatch(1);
		GenericMessage<String> sentMessage = new GenericMessage<String>("hello");
		queueChannel.send(sentMessage);
		assertTrue(service.latch.await(10, TimeUnit.SECONDS));
		Message<?> message = good.receive(1000);
		assertNotNull(message);
		assertEquals("hello", message.getPayload());
		assertSame(message, sentMessage);
	}

	@Test
	public void testRollback() throws Exception {
		service.latch = new CountDownLatch(1);
		queueChannel.send(new GenericMessage<String>("fail"));
		assertTrue(service.latch.await(10, TimeUnit.SECONDS));
		Message<?> message = queueChannel.receive(1000);
		assertNotNull(message);
		assertEquals("retry:fail", message.getPayload());
		assertNull(good.receive(0));
	}

	@Test
	public void testIncludeChannelName() throws Exception {
		service.latch = new CountDownLatch(1);
		Message<String> sentMessage = MessageBuilder.withPayload("hello")
				.setHeader("foo", "bar").build();
		queueChannel2.send(sentMessage);
		assertTrue(service.latch.await(10, TimeUnit.SECONDS));
		Message<?> message = good.receive(1000);
		assertNotNull(message);
		assertEquals("hello processed ok from queueChannel2", message.getPayload());
		assertNotNull(message.getHeaders().get("foo"));
		assertEquals("bar", message.getHeaders().get("foo"));
		}

	public static class Service {
		private CountDownLatch latch;

		public void handle(String foo) {
			latch.countDown();
			if (foo.startsWith("fail")) {
				throw new RuntimeException("planned failure");
			}
		}
	}

}
