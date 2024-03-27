/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Kiel Boatman
 * @author Artem Bilan
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PollingConsumerEndpointTests {

	private final OnlyOnceTrigger trigger = new OnlyOnceTrigger();

	private final TestConsumer consumer = new TestConsumer();

	private final Message message = new GenericMessage<>("test");

	private final Message badMessage = new GenericMessage<>("bad");

	private final TestErrorHandler errorHandler = new TestErrorHandler();

	private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	private PollingConsumer endpoint;

	private PollableChannel channelMock;

	@Before
	public void init() {
		this.channelMock = mock(PollableChannel.class);
		this.endpoint = new PollingConsumer(this.channelMock, this.consumer);
		this.taskScheduler.setPoolSize(5);
		this.endpoint.setErrorHandler(this.errorHandler);
		this.endpoint.setTaskScheduler(this.taskScheduler);
		this.endpoint.setTrigger(this.trigger);
		this.endpoint.setBeanFactory(mock(BeanFactory.class));
		this.endpoint.setReceiveTimeout(-1);
		this.endpoint.afterPropertiesSet();
		this.taskScheduler.afterPropertiesSet();
	}

	@After
	public void stop() {
		taskScheduler.destroy();
	}

	@Test
	public void singleMessage() {
		Mockito.when(this.channelMock.receive()).thenReturn(this.message);
		this.endpoint.setMaxMessagesPerPoll(1);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(1);
	}

	@Test
	public void multipleMessages() {
		Mockito.when(this.channelMock.receive())
				.thenReturn(this.message, this.message, this.message, this.message, this.message);
		this.endpoint.setMaxMessagesPerPoll(5);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(5);
	}

	@Test
	public void multipleMessages_under_run() {
		Mockito.when(this.channelMock.receive())
				.thenReturn(this.message, this.message, this.message, this.message, this.message, null);
		this.endpoint.setMaxMessagesPerPoll(6);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(5);
	}

	@Test
	public void heavierLoadTest() {
		for (int i = 0; i < 1000; i++) {
			init();
			this.trigger.reset();
			this.consumer.counter.set(0);
			multipleMessages();
			stop();
		}
	}

	@Test(expected = MessageRejectedException.class)
	public void rejectedMessage() throws Throwable {
		Mockito.when(this.channelMock.receive()).thenReturn(this.badMessage);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(1);
		this.errorHandler.throwLastErrorIfAvailable();
	}

	@Test(expected = MessageRejectedException.class)
	public void droppedMessage_onePerPoll() throws Throwable {
		Mockito.when(this.channelMock.receive()).thenReturn(this.badMessage);
		this.endpoint.setMaxMessagesPerPoll(10);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(1);
		this.errorHandler.throwLastErrorIfAvailable();
	}

	@Test
	public void blockingSourceTimedOut() {
		// we don't need to await the timeout, returning null suffices
		Mockito.when(this.channelMock.receive()).thenReturn(null);
		this.endpoint.setReceiveTimeout(1);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(0);
	}

	@Test
	public void blockingSourceNotTimedOut() {
		Mockito.when(this.channelMock.receive(Mockito.eq(1L))).thenReturn(this.message);
		this.endpoint.setReceiveTimeout(1);
		this.endpoint.setMaxMessagesPerPoll(1);
		this.endpoint.start();
		this.trigger.await();
		this.endpoint.stop();
		assertThat(this.consumer.counter.get()).isEqualTo(1);
	}

	private static class TestConsumer implements MessageHandler {

		private volatile AtomicInteger counter = new AtomicInteger();

		TestConsumer() {
			super();
		}

		@Override
		public void handleMessage(Message<?> message) {
			this.counter.incrementAndGet();
			if ("bad".equals(message.getPayload().toString())) {
				throw new MessageRejectedException(message, "intentional test failure");
			}
		}

	}

	private static class TestErrorHandler implements ErrorHandler {

		private volatile Throwable lastError;

		TestErrorHandler() {
			super();
		}

		@Override
		public void handleError(Throwable t) {
			this.lastError = t;
		}

		public void throwLastErrorIfAvailable() throws Throwable {
			if (this.lastError instanceof MessagingExceptionWrapper) {
				this.lastError = this.lastError.getCause();
			}
			Throwable t = this.lastError;
			this.lastError = null;
			throw t;
		}

	}

}
