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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class DispatchingChannelErrorHandlingTests {

	private final CountDownLatch latch = new CountDownLatch(1);


	@Test(expected = MessageDeliveryException.class)
	public void handlerThrowsExceptionPublishSubscribeWithoutExecutor() {
		PublishSubscribeChannel channel = new PublishSubscribeChannel();
		channel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new UnsupportedOperationException("intentional test failure");
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
	}

	@Test
	public void handlerThrowsExceptionPublishSubscribeWithExecutor() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel defaultErrorChannel = (DirectChannel) context.getBean(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		PublishSubscribeChannel channel = new PublishSubscribeChannel(executor);
		channel.setBeanFactory(context);
		channel.afterPropertiesSet();
		ResultHandler resultHandler = new ResultHandler();
		defaultErrorChannel.subscribe(resultHandler);
		channel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new MessagingException(message,
						new UnsupportedOperationException("intentional test failure"));
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
		this.waitForLatch(1000);
		Message<?> errorMessage = resultHandler.lastMessage;
		assertEquals(MessagingException.class, errorMessage.getPayload().getClass());
		MessagingException exceptionPayload = (MessagingException) errorMessage.getPayload();
		assertEquals(UnsupportedOperationException.class, exceptionPayload.getCause().getClass());
		assertSame(message, exceptionPayload.getFailedMessage());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
	}

	@Test
	public void handlerThrowsExceptionExecutorChannel() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, DirectChannel.class);
		context.refresh();
		DirectChannel defaultErrorChannel = (DirectChannel) context.getBean(
				IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		TaskExecutor executor = new SimpleAsyncTaskExecutor();
		ExecutorChannel channel = new ExecutorChannel(executor);
		channel.setBeanFactory(context);
		channel.afterPropertiesSet();
		ResultHandler resultHandler = new ResultHandler();
		defaultErrorChannel.subscribe(resultHandler);
		channel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				throw new MessagingException(message,
						new UnsupportedOperationException("intentional test failure"));
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		channel.send(message);
		this.waitForLatch(1000);
		Message<?> errorMessage = resultHandler.lastMessage;
		assertEquals(MessagingException.class, errorMessage.getPayload().getClass());
		MessagingException exceptionPayload = (MessagingException) errorMessage.getPayload();
		assertEquals(UnsupportedOperationException.class, exceptionPayload.getCause().getClass());
		assertSame(message, exceptionPayload.getFailedMessage());
		assertNotSame(Thread.currentThread(), resultHandler.lastThread);
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
