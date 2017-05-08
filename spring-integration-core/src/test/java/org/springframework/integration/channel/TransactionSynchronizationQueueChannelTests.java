/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TransactionSynchronizationQueueChannelTests {

	@Autowired
	private QueueChannel queueChannel;

	@Autowired
	private QueueChannel good;

	@Autowired
	private QueueChannel queueChannel2;

	@Before
	public void setup() {
		this.good.purge(null);
		this.queueChannel.purge(null);
		this.queueChannel2.purge(null);
	}

	@Test
	public void testCommit() throws Exception {
		GenericMessage<String> sentMessage = new GenericMessage<>("hello");
		this.queueChannel.send(sentMessage);
		Message<?> message = this.good.receive(10000);
		assertNotNull(message);
		assertEquals("hello", message.getPayload());
		assertSame(message, sentMessage);
	}

	@Test
	public void testRollback() throws Exception {
		this.queueChannel.send(new GenericMessage<>("fail"));
		Message<?> message = this.good.receive(10000);
		assertNotNull(message);
		assertEquals("retry:fail", message.getPayload());
	}

	@Test
	public void testIncludeChannelName() throws Exception {
		Message<String> sentMessage = MessageBuilder.withPayload("hello")
				.setHeader("foo", "bar").build();
		queueChannel2.send(sentMessage);
		Message<?> message = good.receive(10000);
		assertNotNull(message);
		assertEquals("hello processed ok from queueChannel2", message.getPayload());
		assertNotNull(message.getHeaders().get("foo"));
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	public static class Service {

		public void handle(String foo) {
			if (foo.startsWith("fail")) {
				throw new RuntimeException("planned failure");
			}
		}

	}

}
