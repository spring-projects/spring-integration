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

import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class EventDrivenConsumerTests {

	@Test
	public void testDynamicConcurrency() throws Exception {
		int messagesToSend = 200;
		int concurrency = 1;
		int maxConcurrency = 40;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		final AtomicInteger maxActive = new AtomicInteger(0);
		final AtomicInteger activeSum = new AtomicInteger(0);
		final MessageBus bus = new MessageBus();
		PointToPointChannel channel = new PointToPointChannel();
		MessageEndpoint endpoint = new GenericMessageEndpoint() {
			public void messageReceived(Message message) {
				System.out.println("[count:" + latch.getCount() + "] received: " + message.getPayload());
				counter.incrementAndGet();
				latch.countDown();
				try { Thread.sleep(3); } catch (InterruptedException e) {}
				activeSum.set(activeSum.addAndGet(bus.getActiveCountForEndpoint("testEndpoint")));
				maxActive.set(Math.max(bus.getActiveCountForEndpoint("testEndpoint"), maxActive.get()));
			}
		};
		bus.registerChannel("testChannel", channel);
		bus.registerEndpoint("testEndpoint", endpoint);
		ConsumerPolicy policy = new ConsumerPolicy();
		policy.setConcurrency(concurrency);
		policy.setMaxConcurrency(maxConcurrency);
		policy.setMaxMessagesPerTask(1);
		policy.setRejectionLimit(1);
		policy.setPeriod(0);
		policy.setReceiveTimeout(100);
		Subscription subscription = new Subscription();
		subscription.setChannel("testChannel");
		subscription.setEndpoint("testEndpoint");
		subscription.setPolicy(policy);
		bus.activateSubscription(subscription);
		bus.start();
		for (int i = 0; i < messagesToSend - 110; i++) {
			channel.send(new DocumentMessage(1, "fast-1." + (i+1)));
		}
		int activeCountAfterFirstBurst = bus.getActiveCountForEndpoint("testEndpoint");
		//System.out.println("after-first: " + activeCountAfterFirstBurst);
		for (int i = 0; i < 10; i++) {
			channel.send(new DocumentMessage(1, "slow-1." + (i+1)));
			Thread.sleep(10);
		}
		int activeCountAfterSlowDown = bus.getActiveCountForEndpoint("testEndpoint");
		//System.out.println("after-slowdown: " + activeCountAfterSlowDown);
		for (int i = 0; i < 100; i++) {
			channel.send(new DocumentMessage(1, "fast-2." + (i+1)));
		}
		int activeCountAfterLastBurst = bus.getActiveCountForEndpoint("testEndpoint");
		//System.out.println("after-last: " + activeCountAfterLastBurst);
		latch.await(100, TimeUnit.SECONDS);
		int averageActive = activeSum.get() / messagesToSend;
		assertTrue(activeCountAfterSlowDown < activeCountAfterFirstBurst);
		assertTrue(activeCountAfterLastBurst > activeCountAfterSlowDown);
		assertEquals(messagesToSend, counter.get());
		assertTrue(maxActive.get() > concurrency);
		assertTrue(maxActive.get() <= maxConcurrency);
		assertTrue(averageActive > concurrency);
		assertTrue(averageActive < maxActive.get());
	}

}
