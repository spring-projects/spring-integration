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

package org.springframework.integration.handler;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 1.0.3
 */
public class DelayHandlerTests {

	private final DirectChannel input = new DirectChannel();

	private final DirectChannel output = new DirectChannel();

	private final CountDownLatch latch = new CountDownLatch(1);


	@Before
	public void setChannelNames() {
		input.setBeanName("input");
		output.setBeanName("output");
	}


	@Test
	public void noDelayHeaderAndDefaultDelayIsZero() throws Exception {
		DelayHandler delayHandler = new DelayHandler(0);
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setGroupId("FOO");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		assertSame(message, resultHandler.lastMessage);
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsPositive() throws Exception {
		DelayHandler delayHandler = new DelayHandler(10);
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setGroupId("FOO");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message, resultHandler.lastMessage);
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderAndDefaultDelayWouldTimeout() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsNegativeAndDefaultDelayWouldTimeout() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsInvalidFallsBackToDefaultDelay() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsDateInTheFutureAndDefaultDelayWouldTimeout() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.setGroupId("FOO");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsDateInThePastAndDefaultDelayWouldTimeout() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsNullDateAndDefaultDelayIsZero() throws Exception {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsFutureDateAndTimesOut() throws Exception {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void delayHeaderIsValidStringAndDefaultDelayWouldTimeout() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		ResultHandler resultHandler = new ResultHandler();
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
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
	public void verifyShutdownWithoutWaitingByDefault() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.afterPropertiesSet();
		delayHandler.handleMessage(new GenericMessage<String>("foo"));
		delayHandler.destroy();
		final ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler)
				new DirectFieldAccessor(delayHandler).getPropertyValue("taskScheduler");
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					taskScheduler.getScheduledExecutor().awaitTermination(10000, TimeUnit.MILLISECONDS);
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
	public void verifyShutdownWithWait() throws Exception {
		DelayHandler delayHandler = new DelayHandler(5000);
		delayHandler.setGroupId("FOO");
		delayHandler.setWaitForTasksToCompleteOnShutdown(true);
		delayHandler.afterPropertiesSet();
		delayHandler.handleMessage(new GenericMessage<String>("foo"));
		delayHandler.destroy();
		final ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler)
				new DirectFieldAccessor(delayHandler).getPropertyValue("taskScheduler");
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					taskScheduler.getScheduledExecutor().awaitTermination(10000, TimeUnit.MILLISECONDS);
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

	@Test(expected = MessageDeliveryException.class)
	public void handlerThrowsExceptionWithNoDelay() throws Exception {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		input.subscribe(delayHandler);
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new UnsupportedOperationException("intentional test failure");
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
	}

	@Test
	public void errorChannelHeaderAndHandlerThrowsExceptionWithDelay() throws Exception {
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setDelayHeaderName("delay");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		DirectChannel errorChannel = new DirectChannel();
		ResultHandler resultHandler = new ResultHandler();
		errorChannel.subscribe(resultHandler);
		input.subscribe(delayHandler);
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new UnsupportedOperationException("intentional test failure");
			}
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10")
				.setErrorChannel(errorChannel).build();
		input.send(message);
		this.waitForLatch(1000);
		Message<?> errorMessage = resultHandler.lastMessage;
		assertEquals(MessageHandlingException.class, errorMessage.getPayload().getClass());
		MessageHandlingException exceptionPayload = (MessageHandlingException) errorMessage.getPayload();
		assertSame(message, exceptionPayload.getFailedMessage());
		assertEquals(MessageDeliveryException.class, exceptionPayload.getCause().getClass());
		MessageDeliveryException nestedException = (MessageDeliveryException) exceptionPayload.getCause();
		assertEquals(UnsupportedOperationException.class, nestedException.getCause().getClass());
		assertSame(message, nestedException.getFailedMessage());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void errorChannelNameHeaderAndHandlerThrowsExceptionWithDelay() throws Exception {
		String errorChannelName = "customErrorChannel";
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(errorChannelName, DirectChannel.class);
		context.refresh();
		DirectChannel customErrorChannel = (DirectChannel) context.getBean(errorChannelName);
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setBeanFactory(context);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		ResultHandler resultHandler = new ResultHandler();
		customErrorChannel.subscribe(resultHandler);
		input.subscribe(delayHandler);
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new UnsupportedOperationException("intentional test failure");
			}
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10")
				.setErrorChannelName(errorChannelName).build();
		input.send(message);
		this.waitForLatch(1000);
		Message<?> errorMessage = resultHandler.lastMessage;
		assertEquals(MessageHandlingException.class, errorMessage.getPayload().getClass());
		MessageHandlingException exceptionPayload = (MessageHandlingException) errorMessage.getPayload();
		assertSame(message, exceptionPayload.getFailedMessage());
		assertEquals(MessageDeliveryException.class, exceptionPayload.getCause().getClass());
		MessageDeliveryException nestedException = (MessageDeliveryException) exceptionPayload.getCause();
		assertEquals(UnsupportedOperationException.class, nestedException.getCause().getClass());
		assertSame(message, nestedException.getFailedMessage());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void defaultErrorChannelAndHandlerThrowsExceptionWithDelay() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel defaultErrorChannel = (DirectChannel) context.getBean(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		DelayHandler delayHandler = new DelayHandler(0);
		delayHandler.setGroupId("FOO");
		delayHandler.setBeanFactory(context);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.setOutputChannel(output);
		delayHandler.afterPropertiesSet();
		ResultHandler resultHandler = new ResultHandler();
		defaultErrorChannel.subscribe(resultHandler);
		input.subscribe(delayHandler);
		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new UnsupportedOperationException("intentional test failure");
			}
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10").build();
		input.send(message);
		this.waitForLatch(1000);
		Message<?> errorMessage = resultHandler.lastMessage;
		assertEquals(MessageHandlingException.class, errorMessage.getPayload().getClass());
		MessageHandlingException exceptionPayload = (MessageHandlingException) errorMessage.getPayload();
		assertSame(message, exceptionPayload.getFailedMessage());
		assertEquals(MessageDeliveryException.class, exceptionPayload.getCause().getClass());
		MessageDeliveryException nestedException = (MessageDeliveryException) exceptionPayload.getCause();
		assertEquals(UnsupportedOperationException.class, nestedException.getCause().getClass());
		assertSame(message, nestedException.getFailedMessage());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void reschedulePersistedMessages() throws Exception {
		MessageGroupStore messageGroupStore = new SimpleMessageStore();
		ResultHandler resultHandler = new ResultHandler();
		output.subscribe(resultHandler);

		DelayHandler delayHandler = new DelayHandler(100);
		delayHandler.setMessageStore(messageGroupStore);
		delayHandler.setOutputChannel(output);
		delayHandler.setGroupId("FOO");
		delayHandler.afterPropertiesSet();

		input.subscribe(delayHandler);
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);

		delayHandler.destroy();

		assertTrue(messageGroupStore.getMessageGroup("FOO").size() == 1);
		assertSame(message, messageGroupStore.getMessageGroup("FOO").getOne());

		//restart simulation

		DelayHandler delayHandler1 = new DelayHandler(100);
		delayHandler1.setMessageStore(messageGroupStore);
		delayHandler1.setOutputChannel(output);
		delayHandler1.setGroupId("FOO");
		delayHandler1.afterPropertiesSet();

		this.waitForLatch(1000);

		assertTrue(messageGroupStore.getMessageGroup("FOO").size() == 0);
		assertSame(message, resultHandler.lastMessage);
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
