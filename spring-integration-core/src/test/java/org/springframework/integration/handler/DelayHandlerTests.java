/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 * @since 1.0.3
 */
public class DelayHandlerTests {

	private static final String DELAYER_MESSAGE_GROUP_ID = "testDelayer.messageGroupId";

	private final DirectChannel input = new DirectChannel();

	private final DirectChannel output = new DirectChannel();

	private final CountDownLatch latch = new CountDownLatch(1);

	private ThreadPoolTaskScheduler taskScheduler;

	private DelayHandler delayHandler;

	private final ResultHandler resultHandler = new ResultHandler();

	@Before
	public void setup() {
		input.setBeanName("input");
		output.setBeanName("output");
		taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, taskScheduler);
		delayHandler.setOutputChannel(output);
		delayHandler.setBeanFactory(mock(BeanFactory.class));
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
	}

	@After
	public void tearDown() {
		taskScheduler.destroy();
	}

	private void setDelayExpression() {
		Expression expression = new SpelExpressionParser().parseExpression("headers.delay");
		this.delayHandler.setDelayExpression(expression);
	}

	private void startDelayerHandler() {
		delayHandler.afterPropertiesSet();
		delayHandler.onApplicationEvent(new ContextRefreshedEvent(TestUtils.createTestApplicationContext()));
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsZero() throws Exception {
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsPositive() throws Exception {
		delayHandler.setDefaultDelay(10);
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", 100).build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNegativeAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", -7000).build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsInvalidFallsBackToDefaultDelay() throws Exception {
		delayHandler.setDefaultDelay(5);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "not a number").build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInTheFutureAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", new Date(new Date().getTime() + 150)).build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsDateInThePastAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", new Date(new Date().getTime() - 60 * 1000)).build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void delayHeaderIsNullDateAndDefaultDelayIsZero() throws Exception {
		this.setDelayExpression();
		this.startDelayerHandler();
		Date nullDate = null;
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", nullDate).build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test(expected = TestTimedOutException.class)
	public void delayHeaderIsFutureDateAndTimesOut() throws Exception {
		this.setDelayExpression();
		this.startDelayerHandler();
		Date future = new Date(new Date().getTime() + 60 * 1000);
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", future).build();
		input.send(message);
		waitForLatch(100);
	}

	@Test
	public void delayHeaderIsValidStringAndDefaultDelayWouldTimeout() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "20").build();
		input.send(message);
		waitForLatch(10000);
		assertSame(message.getPayload(), resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void verifyShutdownWithoutWaitingByDefault() throws Exception {
		delayHandler.setDefaultDelay(5000);
		this.startDelayerHandler();
		delayHandler.handleMessage(new GenericMessage<String>("foo"));
		taskScheduler.destroy();

		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(() -> {
			try {
				taskScheduler.getScheduledExecutor().awaitTermination(10000, TimeUnit.MILLISECONDS);
				latch.countDown();
			}
			catch (InterruptedException e) {
				// won't countDown
			}
		}).start();

		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void verifyShutdownWithWait() throws Exception {
		this.delayHandler.setDefaultDelay(100);
		this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
		startDelayerHandler();
		this.delayHandler.handleMessage(new GenericMessage<>("foo"));
		this.taskScheduler.destroy();

		assertTrue(this.taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS));
		assertTrue(this.latch.await(10, TimeUnit.SECONDS));
	}

	@Test(expected = MessageDeliveryException.class)
	public void handlerThrowsExceptionWithNoDelay() throws Exception {
		this.startDelayerHandler();
		output.unsubscribe(resultHandler);
		output.subscribe(message -> {
			throw new UnsupportedOperationException("intentional test failure");
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
		this.setDelayExpression();
		this.startDelayerHandler();
		output.unsubscribe(resultHandler);
		errorChannel.subscribe(resultHandler);
		output.subscribe(message -> {
			throw new UnsupportedOperationException("intentional test failure");
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10")
				.setErrorChannel(errorChannel).build();
		input.send(message);
		waitForLatch(10000);
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
		this.setDelayExpression();
		this.startDelayerHandler();
		output.unsubscribe(resultHandler);
		customErrorChannel.subscribe(resultHandler);
		output.subscribe(message -> {
			throw new UnsupportedOperationException("intentional test failure");
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10")
				.setErrorChannelName(errorChannelName).build();
		input.send(message);
		waitForLatch(10000);
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
		this.setDelayExpression();
		this.startDelayerHandler();
		output.unsubscribe(resultHandler);
		defaultErrorChannel.subscribe(resultHandler);
		output.subscribe(message -> {
			throw new UnsupportedOperationException("intentional test failure");
		});
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "10").build();
		input.send(message);
		waitForLatch(10000);
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
		this.delayHandler.setDefaultDelay(2000);
		this.delayHandler.setMessageStore(messageGroupStore);
		this.startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.input.send(message);

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

		this.taskScheduler.afterPropertiesSet();
		this.delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, this.taskScheduler);
		this.delayHandler.setOutputChannel(output);
		this.delayHandler.setDefaultDelay(200);
		this.delayHandler.setMessageStore(messageGroupStore);
		this.delayHandler.setBeanFactory(mock(BeanFactory.class));
		this.startDelayerHandler();

		waitForLatch(10000);

		assertSame(message.getPayload(), this.resultHandler.lastMessage.getPayload());
		assertNotSame(Thread.currentThread(), this.resultHandler.lastThread);
		assertEquals(1, messageGroupStore.getMessageGroupCount());
		assertEquals(0, messageGroupStore.messageGroupSize(DELAYER_MESSAGE_GROUP_ID));
	}

	@Test //INT-1132
	// Can happen in the parent-child context e.g. Spring-MVC applications
	public void testDoubleOnApplicationEvent() throws Exception {
		this.delayHandler = Mockito.spy(this.delayHandler);
		Mockito.doAnswer(invocation -> null).when(this.delayHandler).reschedulePersistedMessages();

		ContextRefreshedEvent contextRefreshedEvent = new ContextRefreshedEvent(TestUtils.createTestApplicationContext());
		this.delayHandler.onApplicationEvent(contextRefreshedEvent);
		this.delayHandler.onApplicationEvent(contextRefreshedEvent);
		Mockito.verify(this.delayHandler, Mockito.times(1)).reschedulePersistedMessages();
	}

	@Test(expected = MessageHandlingException.class)
	public void testInt2243IgnoreExpressionFailuresAsFalse() throws Exception {
		this.setDelayExpression();
		this.delayHandler.setIgnoreExpressionFailures(false);
		this.startDelayerHandler();
		this.delayHandler.handleMessage(new GenericMessage<String>("test"));
	}

	@Test //INT-3560
	/*
	 It's difficult to test it from real ctx, because any async process from 'inbound-channel-adapter'
	 can't achieve the DelayHandler before the main thread emits 'ContextRefreshedEvent'.
	 */
	public void testRescheduleAndHandleAtTheSameTime() {
		QueueChannel results = new QueueChannel();
		delayHandler.setOutputChannel(results);
		this.delayHandler.setDefaultDelay(10);
		startDelayerHandler();

		this.input.send(new GenericMessage<>("foo"));
		this.delayHandler.reschedulePersistedMessages();
		Message<?> message = results.receive(10000);
		assertNotNull(message);
		message = results.receive(50);
		assertNull(message);
	}

	@Test
	public void testRescheduleForTheDateDelay() throws Exception {
		this.delayHandler.setDelayExpression(new SpelExpressionParser().parseExpression("payload"));
		this.delayHandler.setOutputChannel(new DirectChannel());
		this.delayHandler.setIgnoreExpressionFailures(false);
		startDelayerHandler();
		Calendar releaseDate = Calendar.getInstance();
		releaseDate.add(Calendar.HOUR, 1);
		this.delayHandler.handleMessage(new GenericMessage<>(releaseDate.getTime()));

		// emulate restart
		this.taskScheduler.destroy();
		MessageGroupStore messageStore = TestUtils.getPropertyValue(this.delayHandler, "messageStore",
				MessageGroupStore.class);
		MessageGroup messageGroup = messageStore.getMessageGroup(DELAYER_MESSAGE_GROUP_ID);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();
		DirectFieldAccessor dfa = new DirectFieldAccessor(payload);
		long requestTime = (long) dfa.getPropertyValue("requestDate");
		Calendar requestDate = Calendar.getInstance();
		requestDate.setTimeInMillis(requestTime);
		requestDate.add(Calendar.HOUR, -2);
		dfa.setPropertyValue("requestDate", requestDate.getTimeInMillis());
		this.taskScheduler.afterPropertiesSet();
		this.delayHandler.reschedulePersistedMessages();
		Queue<?> works = TestUtils.getPropertyValue(this.taskScheduler, "scheduledExecutor.workQueue", Queue.class);
		int n = 0;
		while (n++ < 2000 && works.size() == 0) {
			Thread.sleep(10);
		}
		assertEquals(1, works.size());
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

		ResultHandler() {
			super();
		}

		@Override
		public void handleMessage(Message<?> message) {
			this.lastMessage = message;
			this.lastThread = Thread.currentThread();
			latch.countDown();
		}
	}


	@SuppressWarnings("serial")
	private static class TestTimedOutException extends RuntimeException {

		TestTimedOutException() {
			super("timed out while waiting for latch");
		}
	}

}
