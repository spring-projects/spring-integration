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
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class FixedDelayConsumerTests {

	@Test
	public void testAllSentMessagesAreReceivedWithinTimeLimit() throws Exception {
		int messagesToSend = 10;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		SimpleChannel channel = new SimpleChannel();
		MessageHandler handler = TestHandlers.countingCountDownHandler(counter, latch);
		MessageBus bus = new MessageBus();
		bus.initialize();
		bus.registerChannel("testChannel", channel);
		PollingSchedule schedule = new PollingSchedule(5);
		schedule.setFixedRate(false);
		Subscription subscription = new Subscription(channel);
		subscription.setSchedule(schedule);
		bus.registerHandler("testHandler", handler, subscription);
		bus.start();
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals(messagesToSend, counter.get());
	}

	@Test
	public void testTimedOutMessagesAreNotReceived() throws Exception {
		int messagesToSend = 20;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		SimpleChannel channel = new SimpleChannel();
		MessageHandler handler = TestHandlers.countingCountDownHandler(counter, latch);
		MessageBus bus = new MessageBus();
		bus.initialize();
		bus.registerChannel("testChannel", channel);
		PollingSchedule schedule = new PollingSchedule(10);
		schedule.setFixedRate(false);
		Subscription subscription = new Subscription(channel);
		subscription.setSchedule(schedule);
		bus.registerHandler("testHandler", handler, subscription);
		for (int i = 0; i < messagesToSend; i++) {
			channel.send(new GenericMessage<String>(1, "test " + (i+1)));
		}
		bus.start();
		latch.await(100, TimeUnit.MILLISECONDS);
		int count = counter.get();
		assertTrue("received " + count + ", expected less than 20", counter.get() < 20);
		bus.stop();
	}

}
