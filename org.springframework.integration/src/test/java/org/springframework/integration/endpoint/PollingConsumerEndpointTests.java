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

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageConsumer;
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

	private Trigger trigger = new TestTrigger();

	private TestConsumer consumer = new TestConsumer();

	private Message message = new StringMessage("test");

	private Message badMessage = new StringMessage("bad");

	private TestErrorHandler errorHandler = new TestErrorHandler();

	private PollableChannel channelMock = createMock(PollableChannel.class);

	private SimpleTaskScheduler taskScheduler = new SimpleTaskScheduler(new SimpleAsyncTaskExecutor());


	@Before
	public void init() throws InterruptedException {
		endpoint = new PollingConsumerEndpoint(consumer, channelMock);
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
	public void singleMessage() throws InterruptedException {
		expect(channelMock.receive()).andReturn(message);
		expectLastCall();
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
	}

	@Test
	public void multipleMessages() throws InterruptedException {
		expect(channelMock.receive()).andReturn(message).times(5);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(5);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
	}

	@Test
	public void multipleMessages_underrun() throws InterruptedException {
		expect(channelMock.receive()).andReturn(message).times(5);
		expect(channelMock.receive()).andReturn(null);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(6);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
	}

	@Test(expected = MessageRejectedException.class)
	public void rejectedMessage() throws Throwable {
		expect(channelMock.receive()).andReturn(badMessage);
		replay(channelMock);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test(expected = MessageRejectedException.class)
	public void droppedMessage_onePerPoll() throws Throwable {
		expect(channelMock.receive()).andReturn(badMessage).times(1);
		replay(channelMock);
		endpoint.setMaxMessagesPerPoll(10);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
		errorHandler.throwLastErrorIfAvailable();
	}

	@Test(expected = TestTimeoutException.class)
	public void blockingSourceTimedOut() throws InterruptedException {
		// we don't need to await the timeout, returning null suffices
		expect(channelMock.receive(1)).andReturn(null);
		replay(channelMock);
		endpoint.setReceiveTimeout(1);
		endpoint.start();
		try {
			consumer.await(500);
		}
		finally {
			endpoint.stop();
			verify(channelMock);
		}
	}

	@Test
	public void blockingSourceNotTimedOut() throws InterruptedException {
		expect(channelMock.receive(1)).andReturn(message);
		expectLastCall();
		replay(channelMock);
		endpoint.setReceiveTimeout(1);
		endpoint.setMaxMessagesPerPoll(1);
		endpoint.start();
		consumer.await(500);
		endpoint.stop();
		verify(channelMock);
	}


	private static class TestConsumer implements MessageConsumer {

		private volatile CountDownLatch latch = new CountDownLatch(1);

		public void onMessage(Message<?> message) {
			try {
				if ("bad".equals(message.getPayload().toString())) {
					throw new MessageRejectedException(message, "intentional test failure");
				}
			}
			finally {
				this.latch.countDown();
			}
		}

		public void await(long timeout) throws InterruptedException {
			this.latch.await(timeout, TimeUnit.MILLISECONDS);
			if (this.latch.getCount() == 0) {
				this.latch = new CountDownLatch(1);
			}
			else {
				throw new TestTimeoutException();
			}
		}
	}


	private static class TestTrigger implements Trigger {

		private final AtomicBoolean hasRun = new AtomicBoolean();

		public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime) {
			if (!hasRun.getAndSet(true)) {
				return new Date();
			}
			return null;
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


	@SuppressWarnings("serial")
	private static class TestTimeoutException extends RuntimeException {
	}

}
