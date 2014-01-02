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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.Message;
import org.springframework.integration.MessageRejectedException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class PollingConsumerEndpointTests {

	private PollingConsumer endpoint;

	private TestTrigger trigger = new TestTrigger();

	private TestConsumer consumer = new TestConsumer();

	@SuppressWarnings("rawtypes")
	private Message message = new GenericMessage<String>("test");

	@SuppressWarnings("rawtypes")
	private Message badMessage = new GenericMessage<String>("bad");

	private TestErrorHandler errorHandler = new TestErrorHandler();

	private PollableChannel channelMock = Mockito.mock(PollableChannel.class);

	private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();


	@Before
	public void init() throws Exception {
		consumer.counter.set(0);
		trigger.reset();
		endpoint = new PollingConsumer(channelMock, consumer);
		taskScheduler.setPoolSize(5);
		endpoint.setErrorHandler(errorHandler);
		endpoint.setTaskScheduler(taskScheduler);
		endpoint.setTrigger(trigger);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.setReceiveTimeout(-1);
		endpoint.afterPropertiesSet();
		taskScheduler.afterPropertiesSet();
		Mockito.reset(channelMock);
	}

	@After
	public void stop() throws Exception {
		taskScheduler.destroy();
	}


	@Test
	public void singleMessage() {
		Mockito.when(channelMock.receive()).thenReturn(message);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
	}

	@Test
	public void multipleMessages() {
		Mockito.when(channelMock.receive()).thenReturn(message, message, message, message, message);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(5, consumer.counter.get());
	}

	@Test
	public void multipleMessages_underrun() {
		Mockito.when(channelMock.receive()).thenReturn(message, message, message, message, message, null);
		endpoint.setMaxMessagesPerPoll(6);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(5, consumer.counter.get());
	}

	@Test
	public void heavierLoadTest() throws Exception {
		for (int i = 0; i < 1000; i++) {
			this.init();
			this.multipleMessages();
			this.stop();
		}
	}

	@Test(expected = MessageRejectedException.class)
	public void rejectedMessage() throws Throwable {
		Mockito.when(channelMock.receive()).thenReturn(badMessage);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test(expected = MessageRejectedException.class)
	public void droppedMessage_onePerPoll() throws Throwable {
		Mockito.when(channelMock.receive()).thenReturn(badMessage);
		endpoint.setMaxMessagesPerPoll(10);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test
	public void blockingSourceTimedOut() {
		// we don't need to await the timeout, returning null suffices
		Mockito.when(channelMock.receive()).thenReturn(null);
		endpoint.setReceiveTimeout(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(0, consumer.counter.get());
	}

	@Test
	public void blockingSourceNotTimedOut() {
		Mockito.when(channelMock.receive(Mockito.eq(1L))).thenReturn(message);
		endpoint.setReceiveTimeout(1);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
	}


	private static class TestConsumer implements MessageHandler {

		private volatile AtomicInteger counter = new AtomicInteger();

		public void handleMessage(Message<?> message) {
			this.counter.incrementAndGet();
			if ("bad".equals(message.getPayload().toString())) {
				throw new MessageRejectedException(message, "intentional test failure");
			}
		}
	}


	private static class TestTrigger implements Trigger {

		private final AtomicBoolean hasRun = new AtomicBoolean();

		private volatile CountDownLatch latch = new CountDownLatch(1);


		public Date nextExecutionTime(TriggerContext triggerContext) {
			if (!this.hasRun.getAndSet(true)) {
				return new Date();
			}
			this.latch.countDown();
			return null;
		}

		public void reset() {
			this.latch = new CountDownLatch(1);
			this.hasRun.set(false);
		}

		public void await() {
			try {
				this.latch.await(5000, TimeUnit.MILLISECONDS);
				if (latch.getCount() != 0) {
					throw new RuntimeException("test latch.await() did not count down");
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException("test latch.await() interrupted");
			}
		}
	}


	private static class TestErrorHandler implements ErrorHandler {

		private volatile Throwable lastError;

		public void handleError(Throwable t) {
			this.lastError = t;
		}

		public void throwLastErrorIfAvailable() throws Throwable {
			Throwable t = this.lastError;
			this.lastError = null;
			throw t;
		}
	}

}
