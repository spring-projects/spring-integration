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

import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class FixedDelayConsumerTests {

	@Test
	public void testAllSentMessagesAreReceivedWithinTimeLimit() throws Exception {
		int messagesToSend = 20;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		SimpleChannel channel = new SimpleChannel();
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				latch.countDown();
				return null;
			}
		};
		MessageBus bus = new MessageBus();
		bus.initialize();
		bus.registerChannel("testChannel", channel);
		PollingSchedule schedule = new PollingSchedule(10);
		schedule.setFixedRate(false);
		ConcurrencyPolicy concurrencyPolicy = new ConcurrencyPolicy();
		concurrencyPolicy.setCoreSize(1);
		concurrencyPolicy.setMaxSize(1);
		Subscription subscription = new Subscription();
		subscription.setSchedule(schedule);
		subscription.setChannelName("testChannel");
		bus.registerHandler("testHandler", handler, subscription, concurrencyPolicy);
		bus.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals(messagesToSend, counter.get());
	}

	@Test
	public void testTimedOutMessagesAreNotReceived() throws Exception {
		int messagesToSend = 20;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		SimpleChannel channel = new SimpleChannel();
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				counter.incrementAndGet();
				latch.countDown();
				return null;
			}
		};
		MessageBus bus = new MessageBus();
		bus.initialize();
		bus.registerChannel("testChannel", channel);
		PollingSchedule schedule = new PollingSchedule(10);
		schedule.setFixedRate(false);
		Subscription subscription = new Subscription(channel);
		subscription.setChannelName("testChannel");
		subscription.setSchedule(schedule);
		bus.registerHandler("testHandler", handler, subscription);
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		bus.start();
		latch.await(80, TimeUnit.MILLISECONDS);
		int count = counter.get();
		assertTrue("received " + count + ", expected less than 11", counter.get() < 11);
		assertTrue("received " + count + ", expected more than 5", counter.get() > 5);
		bus.stop();
	}

}
