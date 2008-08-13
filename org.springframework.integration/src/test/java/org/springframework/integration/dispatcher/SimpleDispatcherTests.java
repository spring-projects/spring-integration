/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.integration.endpoint.DefaultEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class SimpleDispatcherTests {

	@Test
	public void testSingleMessage() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		dispatcher.addTarget(createEndpoint(TestHandlers.countDownHandler(latch)));
		dispatcher.send(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
	}

	@Test
	public void testPointToPoint() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		dispatcher.addTarget(createEndpoint(TestHandlers.countingCountDownHandler(counter1, latch)));
		dispatcher.addTarget(createEndpoint(TestHandlers.countingCountDownHandler(counter2, latch)));
		dispatcher.send(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("only 1 handler should have received the message", 1, counter1.get() + counter2.get());
	}

	@Test
	public void testHandlersWithSelectorsAndOneAccepts() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		dispatcher.setRejectionLimit(5);
		dispatcher.setRetryInterval(5);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger selectorCounter = new AtomicInteger();
		DefaultEndpoint<?> endpoint1 = createEndpoint(TestHandlers.countingCountDownHandler(counter1, latch));
		DefaultEndpoint<?> endpoint2 = createEndpoint(TestHandlers.countingCountDownHandler(counter2, latch));
		DefaultEndpoint<?> endpoint3 = createEndpoint(TestHandlers.countingCountDownHandler(counter3, latch));
		endpoint1.setSelector(new TestMessageSelector(selectorCounter, false));
		endpoint2.setSelector(new TestMessageSelector(selectorCounter, false));
		endpoint3.setSelector(new TestMessageSelector(selectorCounter, true));
		dispatcher.addTarget(endpoint1);
		dispatcher.addTarget(endpoint2);
		dispatcher.addTarget(endpoint3);
		dispatcher.send(new StringMessage("test"));
		assertEquals(0, latch.getCount());
		assertEquals("selectors should have been invoked one time each", 3, selectorCounter.get());
		assertEquals("handler with rejecting selector should not have received the message", 0, counter1.get());
		assertEquals("handler with rejecting selector should not have received the message", 0, counter2.get());
		assertEquals("handler with accepting selector should have received the message", 1, counter3.get());	
	}

	@Test()
	public void testHandlersWithSelectorsAndNoneAccept() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		dispatcher.setRejectionLimit(5);
		dispatcher.setRetryInterval(5);
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicInteger counter1 = new AtomicInteger();
		final AtomicInteger counter2 = new AtomicInteger();
		final AtomicInteger counter3 = new AtomicInteger();
		final AtomicInteger selectorCounter = new AtomicInteger();
		DefaultEndpoint<?> endpoint1 = createEndpoint(TestHandlers.countingCountDownHandler(counter1, latch));
		DefaultEndpoint<?> endpoint2 = createEndpoint(TestHandlers.countingCountDownHandler(counter2, latch));
		DefaultEndpoint<?> endpoint3 = createEndpoint(TestHandlers.countingCountDownHandler(counter3, latch));
		endpoint1.setSelector(new TestMessageSelector(selectorCounter, false));
		endpoint2.setSelector(new TestMessageSelector(selectorCounter, false));
		endpoint3.setSelector(new TestMessageSelector(selectorCounter, false));
		dispatcher.addTarget(endpoint1);
		dispatcher.addTarget(endpoint2);
		dispatcher.addTarget(endpoint3);
		boolean exceptionThrown = false;
		try {
			dispatcher.send(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		assertEquals("selectors should have been invoked one time each", 3, selectorCounter.get());
		assertEquals("handler with rejecting selector should not have received the message", 0, counter1.get());
		assertEquals("handler with rejecting selector should not have received the message", 0, counter2.get());
		assertEquals("handler with rejecting selector should not have received the message", 0, counter3.get());
	}

	@Test
	public void testHandlersThrowingExceptionUntilRetried() throws InterruptedException {
		SimpleDispatcher dispatcher = new SimpleDispatcher();
		dispatcher.setRejectionLimit(5);
		dispatcher.setRetryInterval(5);
		final AtomicInteger handlerCounter = new AtomicInteger();
		TestMessageHandler handler1 = new TestMessageHandler(handlerCounter, 4);
		TestMessageHandler handler2 = new TestMessageHandler(handlerCounter, 4);
		TestMessageHandler handler3 = new TestMessageHandler(handlerCounter, 2);
		DefaultEndpoint<?> endpoint1 = new DefaultEndpoint<MessageHandler>(handler1);
		DefaultEndpoint<?> endpoint2 = new DefaultEndpoint<MessageHandler>(handler2);
		DefaultEndpoint<?> endpoint3 = new DefaultEndpoint<MessageHandler>(handler3);
		dispatcher.addTarget(endpoint1);
		dispatcher.addTarget(endpoint2);
		dispatcher.addTarget(endpoint3);
		dispatcher.send(new StringMessage("test"));
		assertEquals("handlers should have been invoked 9 times in total", 9, handlerCounter.get());
		assertFalse("first handler should not have handled the message", handler1.handledMessage);
		assertFalse("second handler should not have handled the message", handler2.handledMessage);
		assertTrue("third handler should have handled the message", handler3.handledMessage);
	}


	private static DefaultEndpoint<MessageHandler> createEndpoint(MessageHandler handler) {
		return new DefaultEndpoint<MessageHandler>(handler);
	}


	private static class TestMessageSelector implements MessageSelector {

		private final AtomicInteger counter;

		private final boolean accept;

		TestMessageSelector(AtomicInteger counter, boolean accept) {
			this.counter = counter;
			this.accept = accept;
		}

		public boolean accept(Message<?> message) {
			this.counter.incrementAndGet();
			return this.accept;
		}
	}


	private static class TestMessageHandler implements MessageHandler {

		private final AtomicInteger internalCounter = new AtomicInteger();

		private final AtomicInteger sharedCounter;

		private final int timesToFail;

		private volatile boolean handledMessage = false;

		TestMessageHandler(AtomicInteger sharedCounter, int timesToReject) {
			this.sharedCounter = sharedCounter;
			this.timesToFail = timesToReject;
		}

		public Message<?> handle(Message<?> message) {
			int count = internalCounter.incrementAndGet();
			this.sharedCounter.incrementAndGet();
			if (this.timesToFail == 0) {
				this.handledMessage = true;
				return null;
			}
			if (this.timesToFail < 0) {
				throw new MessageHandlingException(message, "intentional test failure");
			}
			if (count > timesToFail) {
				this.handledMessage = true;
				return null;
			}
			else {
				throw new MessageHandlingException(message, "intentional test failure");
			}
		}
	}

}
