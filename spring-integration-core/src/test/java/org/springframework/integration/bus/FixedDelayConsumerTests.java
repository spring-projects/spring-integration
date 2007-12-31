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

package org.springframework.integration.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 */
public class FixedDelayConsumerTests {

	@Test
	public void temp() {
		// stub method until others can be uncommented
	}

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
		MessageBus bus = new MessageBus();
		bus.registerChannel("testChannel", channel);
		bus.registerEndpoint("testEndpoint", endpoint);
		ConsumerPolicy policy = new ConsumerPolicy();
		policy.setConcurrency(1);
		policy.setMaxConcurrency(1);
		policy.setMaxMessagesPerTask(1);
		policy.setFixedRate(true);
		policy.setPeriod(10);
		Subscription subscription = new Subscription();
		subscription.setChannel("testChannel");
		subscription.setEndpoint("testEndpoint");
		subscription.setPolicy(policy);
		bus.activateSubscription(subscription);
		bus.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		latch.await(5000, TimeUnit.MILLISECONDS);
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
		MessageBus bus = new MessageBus();
		bus.registerChannel("testChannel", channel);
		bus.registerEndpoint("testEndpoint", endpoint);
		ConsumerPolicy policy = new ConsumerPolicy();
		policy.setConcurrency(1);
		policy.setMaxConcurrency(1);
		policy.setMaxMessagesPerTask(1);
		policy.setFixedRate(true);
		policy.setPeriod(10);
		Subscription subscription = new Subscription();
		subscription.setChannel("testChannel");
		subscription.setEndpoint("testEndpoint");
		subscription.setPolicy(policy);
		bus.activateSubscription(subscription);
		bus.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		latch.await(80, TimeUnit.MILLISECONDS);
		assertTrue(counter.get() < 15);
		assertTrue(counter.get() > 5);
	}

}
