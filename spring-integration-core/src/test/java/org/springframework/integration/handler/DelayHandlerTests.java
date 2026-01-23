/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.handler;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 1.0.3
 */
public class DelayHandlerTests implements TestApplicationContextAware {

	private static final String DELAYER_MESSAGE_GROUP_ID = "testDelayer.messageGroupId";

	private final DirectChannel input = new DirectChannel();

	private final DirectChannel output = new DirectChannel();

	private final CountDownLatch latch = new CountDownLatch(1);

	private final ResultHandler resultHandler = new ResultHandler();

	private final TestApplicationContext context = TestUtils.createTestApplicationContext();

	private ThreadPoolTaskScheduler taskScheduler;

	private DelayHandler delayHandler;

	@BeforeEach
	public void setup() {
		input.setBeanName("input");
		output.setBeanName("output");
		taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, taskScheduler);
		delayHandler.setOutputChannel(output);
		delayHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		input.subscribe(delayHandler);
		output.subscribe(resultHandler);
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
		this.taskScheduler.destroy();
	}

	private void setDelayExpression() {
		Expression expression = new SpelExpressionParser().parseExpression("headers.delay");
		this.delayHandler.setDelayExpression(expression);
	}

	private void startDelayerHandler() {
		this.delayHandler.setApplicationContext(this.context);
		this.delayHandler.afterPropertiesSet();
		this.delayHandler.onApplicationEvent(new ContextRefreshedEvent(this.context));
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsZero() {
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isSameAs(Thread.currentThread());
	}

	@Test
	public void noDelayHeaderAndDefaultDelayIsPositive() {
		delayHandler.setDefaultDelay(10);
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void errorFlowAndRetries() throws Exception {
		delayHandler.setDefaultDelay(10);
		delayHandler.setRetryDelay(15);
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("foo", new AtomicInteger())
				.build();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger count = new AtomicInteger();
		delayHandler.setDelayedMessageErrorChannel((m, t) -> {
			count.incrementAndGet();
			int deliveries = StaticMessageHeaderAccessor.getDeliveryAttempt(m).get();
			((MessagingException) m.getPayload())
					.getFailedMessage()
					.getHeaders()
					.get("foo", AtomicInteger.class)
					.incrementAndGet();
			if (deliveries < 3) {
				throw new RuntimeException("fail");
			}
			else if (deliveries == 3) {
				return false;
			}
			else {
				latch.countDown();
				return true;
			}
		});
		delayHandler.setOutputChannel((m, t) -> {
			throw new MessagingException(m);
		});
		input.send(message);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(50);
		assertThat(count.get()).isEqualTo(4);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.delayHandler, "deliveries")).hasSize(0);
	}

	@Test
	public void delayHeaderAndDefaultDelayWouldTimeout() {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", 100).build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsNegativeAndDefaultDelayWouldTimeout() {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", -7000).build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsInvalidFallsBackToDefaultDelay() {
		delayHandler.setDefaultDelay(5);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "not a number").build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsDateInTheFutureAndDefaultDelayWouldTimeout() {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", new Date(new Date().getTime() + 150)).build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsDateInThePastAndDefaultDelayWouldTimeout() {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", new Date(new Date().getTime() - 60 * 1000)).build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsNullDateAndDefaultDelayIsZero() {
		this.setDelayExpression();
		startDelayerHandler();
		Date nullDate = null;
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", nullDate).build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isSameAs(Thread.currentThread());
	}

	@Test
	public void delayHeaderIsFutureDateAndTimesOut() {
		this.setDelayExpression();
		startDelayerHandler();
		Date future = new Date(new Date().getTime() + 60 * 1000);
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", future).build();
		input.send(message);
		assertThatExceptionOfType(TestTimedOutException.class)
				.isThrownBy(() -> waitForLatch(100));
	}

	@Test
	public void delayHeaderIsValidStringAndDefaultDelayWouldTimeout() {
		delayHandler.setDefaultDelay(5000);
		this.setDelayExpression();
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test")
				.setHeader("delay", "20").build();
		input.send(message);
		waitForLatch(10000);
		assertThat(resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void verifyShutdownWithoutWaitingByDefault() throws Exception {
		delayHandler.setDefaultDelay(5000);
		startDelayerHandler();
		delayHandler.handleMessage(new GenericMessage<>("foo"));
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

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void verifyShutdownWithWait() throws Exception {
		this.delayHandler.setDefaultDelay(100);
		this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
		startDelayerHandler();
		this.delayHandler.handleMessage(new GenericMessage<>("foo"));
		this.taskScheduler.destroy();

		assertThat(this.taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void handlerThrowsExceptionWithNoDelay() {
		startDelayerHandler();
		output.unsubscribe(resultHandler);
		output.subscribe(message -> {
			throw new UnsupportedOperationException("intentional test failure");
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> input.send(message));
	}

	@Test
	public void errorChannelHeaderAndHandlerThrowsExceptionWithDelay() {
		this.delayHandler.setRetryDelay(1);
		DirectChannel errorChannel = new DirectChannel();
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setDefaultErrorChannel(errorChannel);
		taskScheduler.setErrorHandler(errorHandler);
		this.setDelayExpression();
		startDelayerHandler();
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
		assertThat(errorMessage.getPayload()).isInstanceOf(MessageDeliveryException.class);
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertThat(exceptionPayload.getFailedMessage().getPayload()).isSameAs(message.getPayload());
		assertThat(exceptionPayload.getCause()).isInstanceOf(UnsupportedOperationException.class);
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void errorChannelNameHeaderAndHandlerThrowsExceptionWithDelay() {
		this.delayHandler.setRetryDelay(1);
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
		startDelayerHandler();
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
		assertThat(errorMessage.getPayload()).isInstanceOf(MessageDeliveryException.class);
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertThat(exceptionPayload.getFailedMessage().getPayload()).isSameAs(message.getPayload());
		assertThat(exceptionPayload.getCause()).isInstanceOf(UnsupportedOperationException.class);
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void defaultErrorChannelAndHandlerThrowsExceptionWithDelay() {
		this.delayHandler.setRetryDelay(1);
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel defaultErrorChannel = (DirectChannel) context
				.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setBeanFactory(context);
		taskScheduler.setErrorHandler(errorHandler);
		this.setDelayExpression();
		startDelayerHandler();
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
		assertThat(errorMessage.getPayload()).isInstanceOf(MessageDeliveryException.class);
		MessageDeliveryException exceptionPayload = (MessageDeliveryException) errorMessage.getPayload();
		assertThat(exceptionPayload.getFailedMessage().getPayload()).isSameAs(message.getPayload());
		assertThat(exceptionPayload.getCause()).isInstanceOf(UnsupportedOperationException.class);
		assertThat(resultHandler.lastThread).isNotSameAs(Thread.currentThread());
	}

	@Test
	public void testReschedulePersistedMessagesOnStartup() throws Exception {
		MessageGroupStore messageGroupStore = new SimpleMessageStore();
		this.delayHandler.setDefaultDelay(2000);
		this.delayHandler.setMessageStore(messageGroupStore);
		startDelayerHandler();
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.input.send(message);

		Thread.sleep(100);

		// emulate restart
		this.taskScheduler.destroy();

		assertThat(messageGroupStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageGroupStore.iterator().next().getGroupId()).isEqualTo(DELAYER_MESSAGE_GROUP_ID);
		assertThat(messageGroupStore.messageGroupSize(DELAYER_MESSAGE_GROUP_ID)).isEqualTo(1);
		assertThat(messageGroupStore.getMessageCountForAllMessageGroups()).isEqualTo(1);
		MessageGroup messageGroup = messageGroupStore.getMessageGroup(DELAYER_MESSAGE_GROUP_ID);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();
		assertThat(payload.getClass().getSimpleName()).isEqualTo("DelayedMessageWrapper");
		assertThat(Objects.requireNonNull(TestUtils.<Object>getPropertyValue(payload, "original.payload")))
				.isEqualTo(message.getPayload());

		this.taskScheduler.afterPropertiesSet();
		this.delayHandler = new DelayHandler(DELAYER_MESSAGE_GROUP_ID, this.taskScheduler);
		this.delayHandler.setOutputChannel(output);
		this.delayHandler.setDefaultDelay(200);
		this.delayHandler.setMessageStore(messageGroupStore);
		this.delayHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		startDelayerHandler();

		waitForLatch(10000);

		assertThat(this.resultHandler.lastMessage.getPayload()).isSameAs(message.getPayload());
		assertThat(this.resultHandler.lastThread).isNotSameAs(Thread.currentThread());
		assertThat(messageGroupStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageGroupStore.messageGroupSize(DELAYER_MESSAGE_GROUP_ID)).isEqualTo(0);
	}

	@Test //INT-1132
	// Can happen in the parent-child context e.g. Spring-MVC applications
	public void testDoubleOnApplicationEvent() {
		this.delayHandler = Mockito.spy(this.delayHandler);
		Mockito.doAnswer(invocation -> null).when(this.delayHandler).reschedulePersistedMessages();

		TestApplicationContext ac = TestUtils.createTestApplicationContext();
		this.delayHandler.setApplicationContext(ac);
		ContextRefreshedEvent contextRefreshedEvent = new ContextRefreshedEvent(ac);
		this.delayHandler.onApplicationEvent(contextRefreshedEvent);
		this.delayHandler.onApplicationEvent(contextRefreshedEvent);
		Mockito.verify(this.delayHandler, Mockito.times(1)).reschedulePersistedMessages();
		ac.close();
	}

	@Test
	public void testInt2243IgnoreExpressionFailuresAsFalse() {
		this.setDelayExpression();
		this.delayHandler.setIgnoreExpressionFailures(false);
		startDelayerHandler();
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.delayHandler.handleMessage(new GenericMessage<>("test")));
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
		assertThat(message).isNotNull();
		message = results.receive(50);
		assertThat(message).isNull();
	}

	@Test
	public void testRescheduleForTheDateDelay() {
		this.delayHandler.setDelayExpression(new SpelExpressionParser().parseExpression("payload"));
		this.delayHandler.setOutputChannel(new DirectChannel());
		this.delayHandler.setIgnoreExpressionFailures(false);
		startDelayerHandler();
		Calendar releaseDate = Calendar.getInstance();
		releaseDate.add(Calendar.HOUR, 1);
		this.delayHandler.handleMessage(new GenericMessage<>(releaseDate.getTime()));

		// emulate restart
		this.taskScheduler.destroy();
		MessageGroupStore messageStore = TestUtils.getPropertyValue(this.delayHandler, "messageStore");
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
		Queue<?> works = ((ScheduledThreadPoolExecutor) this.taskScheduler.getScheduledExecutor()).getQueue();

		await()
				.atMost(Duration.ofSeconds(20))
				.pollDelay(Duration.ofMillis(10))
				.until(() -> works.size() == 1);
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
