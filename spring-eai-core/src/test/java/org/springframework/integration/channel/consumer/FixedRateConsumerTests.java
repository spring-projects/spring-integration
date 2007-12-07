/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class FixedRateConsumerTests {

	@Test
	public void testAllSentMessagesAreReceivedWithinTimeLimit() throws Exception {
		int messagesToSend = 20;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		PointToPointChannel channel = new PointToPointChannel();
		MessageEndpoint endpoint = new GenericMessageEndpoint() {
			public void messageReceived(Message message) {
				counter.incrementAndGet();
				latch.countDown();
			}
		};
		FixedRateConsumer consumer = new FixedRateConsumer(channel, endpoint);
		consumer.setPollInterval(10);
		consumer.initialize();
		consumer.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new DocumentMessage(1, "test " + (i+1)));
		}
		latch.await(250, TimeUnit.MILLISECONDS);
		assertEquals(messagesToSend, counter.get());
	}

	@Test
	public void testTimedOutMessagesAreNotReceived() throws Exception {
		int messagesToSend = 20;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		PointToPointChannel channel = new PointToPointChannel();
		MessageEndpoint endpoint = new GenericMessageEndpoint() {
			public void messageReceived(Message message) {
				counter.incrementAndGet();
				latch.countDown();
			}
		};
		FixedRateConsumer consumer = new FixedRateConsumer(channel, endpoint);
		consumer.setPollInterval(10);
		consumer.initialize();
		consumer.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new DocumentMessage(1, "test " + (i+1)));
		}
		latch.await(80, TimeUnit.MILLISECONDS);
		assertTrue(counter.get() < 10);
		assertTrue(counter.get() > 7);
	}

}
