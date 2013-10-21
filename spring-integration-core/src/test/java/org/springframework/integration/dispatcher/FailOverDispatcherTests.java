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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.message.TestHandlers;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;

/**
 * @author Mark Fisher
 */
public class FailOverDispatcherTests {

	@Test
	public void singleMessage() throws InterruptedException {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		dispatcher.addHandler(createConsumer(TestHandlers.countDownHandler(latch)));
		dispatcher.dispatch(new GenericMessage<String>("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
	}

	@Test
	public void pointToPoint() throws InterruptedException {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		dispatcher.addHandler(createConsumer(TestHandlers.countingCountDownHandler(counter1, latch)));
		dispatcher.addHandler(createConsumer(TestHandlers.countingCountDownHandler(counter2, latch)));
		dispatcher.dispatch(new GenericMessage<String>("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("only 1 handler should have received the message", 1, counter1.get() + counter2.get());
	}

	@Test
	public void noDuplicateSubscriptions() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target);
		dispatcher.addHandler(target);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals("target should not have duplicate subscriptions", 1, counter.get());
	}

	@Test
	public void removeConsumerBeforeSend() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		dispatcher.removeHandler(target2);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(2, counter.get());
	}

	@Test
	public void removeConsumerBetweenSends() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test1"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(3, counter.get());
		dispatcher.removeHandler(target2);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test2"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(5, counter.get());
		dispatcher.removeHandler(target1);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test3"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(6, counter.get());
	}

	@Test(expected = MessageDeliveryException.class)
	public void removeConsumerLastTargetCausesDeliveryException() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target);
		try {
			dispatcher.dispatch(new GenericMessage<String>("test1"));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals(1, counter.get());
		dispatcher.removeHandler(target);
		dispatcher.dispatch(new GenericMessage<String>("test2"));
	}

	@Test
	public void firstHandlerReturnsTrue() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, true);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertTrue(dispatcher.dispatch(new GenericMessage<String>("test")));
		assertEquals("only the first target should have been invoked", 1, counter.get());
	}

	@Test
	public void middleHandlerReturnsTrue() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, true);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		assertTrue(dispatcher.dispatch(new GenericMessage<String>("test")));
		assertEquals("first two targets should have been invoked", 2, counter.get());
	}

	@Test
	public void allHandlersReturnFalse() {
		UnicastingDispatcher dispatcher = new UnicastingDispatcher();
		final AtomicInteger counter = new AtomicInteger();
		MessageHandler target1 = new CountingTestEndpoint(counter, false);
		MessageHandler target2 = new CountingTestEndpoint(counter, false);
		MessageHandler target3 = new CountingTestEndpoint(counter, false);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		try {
			assertFalse(dispatcher.dispatch(new GenericMessage<String>("test")));
		}
		catch (Exception e) {
			// ignore
		}
		assertEquals("each target should have been invoked", 3, counter.get());
	}


	private static ServiceActivatingHandler createConsumer(Object object) {
		return new ServiceActivatingHandler(object);
	}


	private static class CountingTestEndpoint implements MessageHandler {

		private final AtomicInteger counter;

		private final boolean shouldAccept;

		CountingTestEndpoint(AtomicInteger counter, boolean shouldAccept) {
			this.counter = counter;
			this.shouldAccept = shouldAccept;
		}

		public void handleMessage(Message<?> message) {
			this.counter.incrementAndGet();
			if (!this.shouldAccept) {
				throw new MessageRejectedException(message, "intentional test failure");
			}
		}
	}

}
