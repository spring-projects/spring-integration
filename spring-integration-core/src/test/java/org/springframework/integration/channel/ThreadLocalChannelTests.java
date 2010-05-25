/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ThreadLocalChannelTests {

	@Before
	public void clearThreadLocalQueue() {
		ThreadLocalChannel channel = new ThreadLocalChannel();
		Message<?> result = null;
		do {
			result = channel.receive(0);
		} while (result != null);
	}


	@Test
	public void testSendAndReceive() {
		ThreadLocalChannel channel = new ThreadLocalChannel();
		StringMessage message = new StringMessage("test");
		assertNull(channel.receive());
		assertTrue(channel.send(message));
		Message<?> response = channel.receive();
		assertNotNull(response);
		assertEquals(response, message);
		assertNull(channel.receive());
	}

	@Test
	public void testSendAndReceiveMultipleMessages() {
		ThreadLocalChannel channel = new ThreadLocalChannel();
		StringMessage message1 = new StringMessage("test1");
		StringMessage message2 = new StringMessage("test2");
		assertNull(channel.receive());
		assertTrue(channel.send(message1));
		assertTrue(channel.send(message2));
		List<Message<?>> receivedMessages = new ArrayList<Message<?>>();
		receivedMessages.add(channel.receive(0));
		receivedMessages.add(channel.receive(0));
		assertEquals(2, receivedMessages.size());
		assertEquals(message1, receivedMessages.get(0));
		assertEquals(message2, receivedMessages.get(1));
		assertNull(channel.receive());
	}

	@Test
	public void multipleThreadLocalChannels() throws Exception {
		final ThreadLocalChannel channel1 = new ThreadLocalChannel();
		final ThreadLocalChannel channel2 = new ThreadLocalChannel();
		channel1.send(new StringMessage("test-1.1"));
		channel1.send(new StringMessage("test-1.2"));
		channel1.send(new StringMessage("test-1.3"));
		channel2.send(new StringMessage("test-2.1"));
		channel2.send(new StringMessage("test-2.2"));
		Executor otherThreadExecutor = Executors.newSingleThreadExecutor();
		final List<Object> otherThreadResults = new ArrayList<Object>();
		final CountDownLatch latch = new CountDownLatch(2);
		otherThreadExecutor.execute(new Runnable() {
			public void run() {
				otherThreadResults.add(channel1.receive(0));
				latch.countDown();
			}
		});
		otherThreadExecutor.execute(new Runnable() {
			public void run() {
				otherThreadResults.add(channel2.receive(0));
				latch.countDown();
			}
		});
		latch.await(1, TimeUnit.SECONDS);
		assertEquals(2, otherThreadResults.size());
		assertNull(otherThreadResults.get(0));
		assertNull(otherThreadResults.get(1));
		assertEquals("test-1.1", channel1.receive(0).getPayload());
		assertEquals("test-1.2", channel1.receive(0).getPayload());
		assertEquals("test-1.3", channel1.receive(0).getPayload());
		assertNull(channel1.receive(0));
		assertEquals("test-2.1", channel2.receive(0).getPayload());
		assertEquals("test-2.2", channel2.receive(0).getPayload());
		assertNull(channel2.receive(0));
	}

}
