/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class DelayHandlerTests {

	private final DirectChannel input = new DirectChannel();

	private final DirectChannel output = new DirectChannel();

	private final CountDownLatch latch = new CountDownLatch(1);


	@Test
	public void noDelayHeaderAndDefaultDelayIsZero() {
		DelayHandler delayHandler = new DelayHandler(0);
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsPositive() {
		DelayHandler delayHandler = new DelayHandler(10);
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderAndDefaultDelayWouldTimeout() {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", 100).build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNegativeAndDefaultDelayWouldTimeout() {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", -7000).build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsInvalidFallsBackToDefaultDelay() {
		DelayHandler delayHandler = new DelayHandler(5);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", "not a number").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInTheFutureAndDefaultDelayWouldTimeout() {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", new Date(new Date().getTime() + 150)).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInThePastAndDefaultDelayWouldTimeout() {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", new Date(new Date().getTime() - 60 * 1000)).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNullDateAndDefaultDelayIsZero() {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Date nullDate = null;
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", nullDate).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test(expected = TestTimedOutException.class)
	public void delayHeaderIsFutureDateAndTimesOut() {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Date future = new Date(new Date().getTime() + 60 * 1000);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", future).build();
		input.send(message);
		this.waitForLatch(50);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsValidStringAndDefaultDelayWouldTimeout() {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", "20").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void verifyShutdownWithoutWaitingByDefault() throws InterruptedException {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.handleMessage(new StringMessage("foo"));
		delayHandler.destroy();
		final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)
				new DirectFieldAccessor(delayHandler).getPropertyValue("scheduler");
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					scheduler.awaitTermination(10000, TimeUnit.MILLISECONDS);
					latch.countDown();
				}
				catch (InterruptedException e) {
					// won't countDown
				}
			}
		}).start();
		latch.await(50, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
	}

	@Test
	public void verifyShutdownWithWait() throws InterruptedException {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setWaitForTasksToCompleteOnShutdown(true);
		delayHandler.handleMessage(new StringMessage("foo"));
		delayHandler.destroy();
		final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)
				new DirectFieldAccessor(delayHandler).getPropertyValue("scheduler");
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					scheduler.awaitTermination(10000, TimeUnit.MILLISECONDS);
					latch.countDown();
				}
				catch (InterruptedException e) {
					// won't countDown
				}
			}
		}).start();
		latch.await(50, TimeUnit.MILLISECONDS);
		assertEquals(1, latch.getCount());
	}


	private void waitForLatch(long timeout) {
		try {
			this.latch.await(timeout, TimeUnit.MILLISECONDS);
			if (latch.getCount() != 0) {
				throw new TestTimedOutException();
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException("interrupted while waiting for latch");
		}
	}


	private class ResultHandler implements MessageHandler {

		private volatile Message<?> lastMessage;

		private volatile Thread lastThread;

		public void handleMessage(Message<?> message) {
			this.lastMessage = message;
			this.lastThread = Thread.currentThread();
			latch.countDown();
		}
	}


	@SuppressWarnings("serial")
	private static class TestTimedOutException extends RuntimeException {

		public TestTimedOutException() {
			super("timed out while waiting for latch");
		}
	}

}
