/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 1.0.3
 */
public class DelayHandlerTests {

	private static final String DELAYER_MESSAGE_GROUP_ID = "testDelayer.messageGroupId";

	private final DirectChannel input = new DirectChannel();

	private final DirectChannel output = new DirectChannel();

	private final CountDownLatch latch = new CountDownLatch(1);

	private ThreadPoolTaskScheduler taskScheduler;

	private DelayHandler delayHandler;

	private ResultHandler resultHandler = new ResultHandler();

	@Before
	public void setup() {
		input.setBeanName("input");
		output.setBeanName("output");
		taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, taskScheduler);
		delayHandler.setOutputChannel(output);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsZero() throws Exception {
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsPositive() throws Exception {
		delayHandler.setDefaultDelay(10);
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", 100).build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNegativeAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", -7000).build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsInvalidFallsBackToDefaultDelay() throws Exception {
		delayHandler.setDefaultDelay(5);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", "not a number").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInTheFutureAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", new Date(new Date().getTime() + 150)).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInThePastAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", new Date(new Date().getTime() - 60 * 1000)).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNullDateAndDefaultDelayIsZero() throws Exception {
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Date nullDate = null;
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", nullDate).build();
		input.send(message);
		this.waitForLatch(3000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test(expected = TestTimedOutException.class)
	public void delayHeaderIsFutureDateAndTimesOut() throws Exception {
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Date future = new Date(new Date().getTime() + 60 * 1000);
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", future).build();
		input.send(message);
		this.waitForLatch(50);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsValidStringAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
			.setHeader("delay", "20").build();
		input.send(message);
		this.waitForLatch(1000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void verifyShutdownWithoutWaitingByDefault() throws Exception {
		delayHandler.setDefaultDelay(5000);
		delayHandler.afterPropertiesSet();
		delayHandler.handleMessage(new GenericMessage<String>("foo"));
		taskScheduler.destroy();

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
		delayHandler.setDefaultDelay(5000);
		taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
		delayHandler.afterPropertiesSet();
		delayHandler.handleMessage(new GenericMessage<String>("foo"));
		taskScheduler.destroy();

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
		delayHandler.afterPropertiesSet();
		output.unsubscribe(resultHandler);
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
		DirectChannel errorChannel = new DirectChannel();
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setDefaultErrorChannel(errorChannel);
		taskScheduler.setErrorHandler(errorHandler);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		output.unsubscribe(resultHandler);
		errorChannel.subscribe(resultHandler);
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
		assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertSame(message.getPayload(), exceptionPayload.getFailedMessage().getPayload());
		assertEquals(UnsupportedOperationException.class, exceptionPayload.getCause().getClass());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void errorChannelNameHeaderAndHandlerThrowsExceptionWithDelay() throws Exception {
		String errorChannelName = "customErrorChannel";
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(errorChannelName, DirectChannel.class);
		context.registerSingleton(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel customErrorChannel = (DirectChannel) context.getBean(errorChannelName);
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setBeanFactory(context);
		taskScheduler.setErrorHandler(errorHandler);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		output.unsubscribe(resultHandler);
		customErrorChannel.subscribe(resultHandler);
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
		assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertSame(message.getPayload(), exceptionPayload.getFailedMessage().getPayload());
		assertEquals(UnsupportedOperationException.class, exceptionPayload.getCause().getClass());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void defaultErrorChannelAndHandlerThrowsExceptionWithDelay() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel defaultErrorChannel = (DirectChannel) context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setBeanFactory(context);
		taskScheduler.setErrorHandler(errorHandler);
		delayHandler.setDelayHeaderName("delay");
		delayHandler.afterPropertiesSet();
		output.unsubscribe(resultHandler);
		defaultErrorChannel.subscribe(resultHandler);
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
		assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertSame(message.getPayload(), exceptionPayload.getFailedMessage().getPayload());
		assertEquals(UnsupportedOperationException.class, exceptionPayload.getCause().getClass());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}


	@Test //INT-1132
	public void testReschedulePersistedMessagesOnStartup() throws Exception {
		MessageGroupStore messageGroupStore = new SimpleMessageStore();
		this.delayHandler.setDefaultDelay(200);
		this.delayHandler.setMessageStore(messageGroupStore);
		this.delayHandler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);

		Thread.sleep(100);

		// emulate restart
		this.taskScheduler.destroy();

		assertEquals(1, messageGroupStore.getMessageGroupCount());
		assertEquals(DELAYER_MESSAGE_GROUP_ID, messageGroupStore.iterator().next().getGroupId());
		assertEquals(1, messageGroupStore.messageGroupSize(DELAYER_MESSAGE_GROUP_ID));
		assertEquals(1, messageGroupStore.getMessageCountForAllMessageGroups());
		MessageGroup messageGroup = messageGroupStore.getMessageGroup(DELAYER_MESSAGE_GROUP_ID);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();
		assertEquals("DelayedMessageWrapper", payload.getClass().getSimpleName());
		assertEquals(message.getPayload(), TestUtils.getPropertyValue(payload, "original.payload"));

		taskScheduler.afterPropertiesSet();
		DelayHandler delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, taskScheduler);
		delayHandler.setOutputChannel(output);
		delayHandler.setDefaultDelay(200);
		delayHandler.setMessageStore(messageGroupStore);
		delayHandler.afterPropertiesSet();
		delayHandler.onApplicationEvent(new ContextRefreshedEvent(TestUtils.createTestApplicationContext()));

		long timeBeforeReceive = System.currentTimeMillis();
		this.latch.await();
		long timeAfterReceive = System.currentTimeMillis();
		assertThat(timeAfterReceive - timeBeforeReceive, Matchers.lessThanOrEqualTo(100L));

		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
		assertEquals(1, messageGroupStore.getMessageGroupCount());
		assertEquals(0, messageGroupStore.messageGroupSize(DELAYER_MESSAGE_GROUP_ID));
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
