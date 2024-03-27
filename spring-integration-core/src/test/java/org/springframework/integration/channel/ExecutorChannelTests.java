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

package org.springframework.integration.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ExecutorChannelTests {

	@Test
	public void verifyDifferentThread() throws Exception {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setThreadNamePrefix("test-");
		ExecutorChannel channel = new ExecutorChannel(taskExecutor);
		CountDownLatch latch = new CountDownLatch(1);
		TestHandler handler = new TestHandler(latch);
		channel.subscribe(handler);
		channel.send(new GenericMessage<>("test"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount()).isEqualTo(0);
		assertThat(handler.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler.thread)).isFalse();
		assertThat(handler.thread.getName()).isEqualTo("test-1");
	}

	@Test
	public void roundRobinLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ScheduledExecutorService exec = Executors
				.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-"));
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(exec);
		ExecutorChannel channel = new ExecutorChannel(
				taskExecutor, new RoundRobinLoadBalancingStrategy());
		CountDownLatch latch = new CountDownLatch(numberOfMessages);
		TestHandler handler1 = new TestHandler(latch);
		TestHandler handler2 = new TestHandler(latch);
		TestHandler handler3 = new TestHandler(latch);
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.subscribe(handler3);
		for (int i = 0; i < numberOfMessages; i++) {
			channel.send(new GenericMessage<>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount()).isEqualTo(0);
		assertThat(handler1.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler1.thread)).isFalse();
		assertThat(handler1.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler2.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler2.thread)).isFalse();
		assertThat(handler2.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler3.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler3.thread)).isFalse();
		assertThat(handler3.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler1.count.get()).isEqualTo(4);
		assertThat(handler2.count.get()).isEqualTo(4);
		assertThat(handler3.count.get()).isEqualTo(3);
		exec.shutdownNow();
	}

	@Test
	public void verifyFailoverWithLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ScheduledExecutorService exec = Executors
				.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-"));
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(exec);
		ExecutorChannel channel = new ExecutorChannel(
				taskExecutor, new RoundRobinLoadBalancingStrategy());
		CountDownLatch latch = new CountDownLatch(numberOfMessages);
		TestHandler handler1 = new TestHandler(latch);
		TestHandler handler2 = new TestHandler(latch);
		TestHandler handler3 = new TestHandler(latch);
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.subscribe(handler3);
		handler2.shouldFail = true;
		for (int i = 0; i < numberOfMessages; i++) {
			channel.send(new GenericMessage<>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount()).isEqualTo(0);
		assertThat(handler1.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler1.thread)).isFalse();
		assertThat(handler1.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler2.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler2.thread)).isFalse();
		assertThat(handler2.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler3.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler3.thread)).isFalse();
		assertThat(handler3.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler2.count.get()).isEqualTo(0);
		assertThat(handler1.count.get()).isEqualTo(4);
		assertThat(handler3.count.get()).isEqualTo(7);
		exec.shutdownNow();
	}

	@Test
	public void verifyFailoverWithoutLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ScheduledExecutorService exec = Executors
				.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-"));
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(exec);
		ExecutorChannel channel = new ExecutorChannel(taskExecutor, null);
		CountDownLatch latch = new CountDownLatch(numberOfMessages);
		TestHandler handler1 = new TestHandler(latch);
		TestHandler handler2 = new TestHandler(latch);
		TestHandler handler3 = new TestHandler(latch);
		channel.subscribe(handler1);
		channel.subscribe(handler2);
		channel.subscribe(handler3);
		handler1.shouldFail = true;
		for (int i = 0; i < numberOfMessages; i++) {
			channel.send(new GenericMessage<>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount()).isEqualTo(0);
		assertThat(handler1.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler1.thread)).isFalse();
		assertThat(handler1.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler2.thread).isNotNull();
		assertThat(Thread.currentThread().equals(handler2.thread)).isFalse();
		assertThat(handler2.thread.getName().startsWith("test-")).isTrue();
		assertThat(handler3.thread).isNull();
		assertThat(handler1.count.get()).isEqualTo(0);
		assertThat(handler3.count.get()).isEqualTo(0);
		assertThat(handler2.count.get()).isEqualTo(numberOfMessages);
		exec.shutdownNow();
	}

	@Test
	public void interceptorWithModifiedMessage() {
		ExecutorChannel channel = new ExecutorChannel(new SyncTaskExecutor());
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();

		MessageHandler handler = mock(MessageHandler.class);
		Message<?> expected = mock(Message.class);
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		interceptor.setMessageToReturn(expected);
		channel.addInterceptor(interceptor);
		channel.subscribe(handler);
		channel.send(new GenericMessage<Object>("foo"));
		verify(handler).handleMessage(expected);
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
	}

	@Test
	public void interceptorWithException() {
		ExecutorChannel channel = new ExecutorChannel(new SyncTaskExecutor());
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();

		Message<Object> message = new GenericMessage<>("foo");

		MessageHandler handler = mock(MessageHandler.class);
		IllegalStateException expected = new IllegalStateException("Fake exception");
		willThrow(expected).given(handler).handleMessage(message);
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		channel.addInterceptor(interceptor);
		channel.subscribe(handler);
		try {
			channel.send(message);
		}
		catch (MessageDeliveryException actual) {
			assertThat(actual.getCause()).isSameAs(expected);
		}
		verify(handler).handleMessage(message);
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
	}

	@Test
	public void testEarlySubscribe() {
		ExecutorChannel channel = new ExecutorChannel(mock(Executor.class));
		try {
			channel.subscribe(m -> {
			});
			channel.setBeanFactory(mock(BeanFactory.class));
			channel.afterPropertiesSet();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage()).isEqualTo("You cannot subscribe() until the channel "
					+ "bean is fully initialized by the framework. Do not subscribe in a @Bean definition");
		}
	}

	private static class TestHandler implements MessageHandler {

		private final CountDownLatch latch;

		private final AtomicInteger count = new AtomicInteger();

		private volatile Thread thread;

		private volatile boolean shouldFail;

		TestHandler(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void handleMessage(Message<?> message) {
			this.thread = Thread.currentThread();
			if (this.shouldFail) {
				throw new RuntimeException("intentional test failure");
			}
			this.count.incrementAndGet();
			this.latch.countDown();
		}

	}

	private static class BeforeHandleInterceptor implements ExecutorChannelInterceptor {

		private final AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterHandledInvoked;

		private Message<?> messageToReturn;

		BeforeHandleInterceptor() {
			super();
		}

		public void setMessageToReturn(Message<?> messageToReturn) {
			this.messageToReturn = messageToReturn;
		}

		public AtomicInteger getCounter() {
			return this.counter;
		}

		public boolean wasAfterHandledInvoked() {
			return this.afterHandledInvoked;
		}

		@Override
		public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
			assertThat(message).isNotNull();
			this.counter.incrementAndGet();
			return (this.messageToReturn != null ? this.messageToReturn : message);
		}

		@Override
		public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler,
				Exception ex) {

			this.afterHandledInvoked = true;
		}

	}

}
