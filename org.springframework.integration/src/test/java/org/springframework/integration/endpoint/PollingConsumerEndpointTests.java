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

package org.springframework.integration.endpoint;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.integration.util.ErrorHandler;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class PollingConsumerEndpointTests {

	private PollingConsumerEndpoint endpoint;

	private TestTrigger trigger = new TestTrigger();

	private TestConsumer consumer = new TestConsumer();

	private Message message = new StringMessage("test");

	private Message badMessage = new StringMessage("bad");

	private TestErrorHandler errorHandler = new TestErrorHandler();

	private PollableChannel channelMock = createMock(PollableChannel.class);

	private SimpleTaskScheduler taskScheduler = new SimpleTaskScheduler(new SimpleAsyncTaskExecutor());


	@Before
	public void init() throws InterruptedException {
		consumer.counter.set(0);
		trigger.reset();
		endpoint = new PollingConsumerEndpoint(channelMock, consumer);
		endpoint.setTaskScheduler(taskScheduler);
		taskScheduler.setErrorHandler(errorHandler);
		taskScheduler.start();
		endpoint.setTrigger(trigger);
		endpoint.setReceiveTimeout(-1);
		reset(channelMock);
	}

	@After
	public void stop() {
		taskScheduler.stop();
	}


	@Test
	public void singleMessage() {
		expect(channelMock.receive()).andReturn(message);
		expectLastCall();
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
		verify(channelMock);
	}

	@Test
	public void multipleMessages() {
		expect(channelMock.receive()).andReturn(message).times(5);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(5, consumer.counter.get());
		verify(channelMock);
	}

	@Test
	public void multipleMessages_underrun() {
		expect(channelMock.receive()).andReturn(message).times(5);
		expect(channelMock.receive()).andReturn(null);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(6);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(5, consumer.counter.get());
		verify(channelMock);
	}

	@Test(expected = MessageRejectedException.class)
	public void rejectedMessage() throws Throwable {
		expect(channelMock.receive()).andReturn(badMessage);
		replay(channelMock);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		verify(channelMock);
		assertEquals(1, consumer.counter.get());
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test(expected = MessageRejectedException.class)
	public void droppedMessage_onePerPoll() throws Throwable {
		expect(channelMock.receive()).andReturn(badMessage).times(1);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(10);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		verify(channelMock);
		assertEquals(1, consumer.counter.get());
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test
	public void blockingSourceTimedOut() {
		// we don't need to await the timeout, returning null suffices
		expect(channelMock.receive(1)).andReturn(null);
		replay(channelMock);
		endpoint.setReceiveTimeout(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(0, consumer.counter.get());
		verify(channelMock);
	}

	@Test
	public void blockingSourceNotTimedOut() {
		expect(channelMock.receive(1)).andReturn(message);
		expectLastCall();
		replay(channelMock);
		endpoint.setReceiveTimeout(1);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		trigger.await();
		endpoint.stop();
		assertEquals(1, consumer.counter.get());
		verify(channelMock);
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


		public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime) {
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

		public void handle(Throwable t) {
			this.lastError = t;
		}

		public void throwLastErrorIfAvailable() throws Throwable {
			Throwable t = this.lastError;
			this.lastError = null;
			throw t;
		}
	}

}
