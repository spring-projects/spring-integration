/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class AsyncHandlerTests {

	private final QueueChannel output = new QueueChannel();

	private AbstractReplyProducingMessageHandler handler;

	private volatile CountDownLatch latch;

	private volatile int whichTest;

	private volatile Exception failedCallbackException;

	private volatile CountDownLatch exceptionLatch = new CountDownLatch(1);

	private volatile CountDownLatch noOutputChannelLatch = new CountDownLatch(1);

	@Before
	public void setup() {
		this.handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				final SettableListenableFuture<String> future = new SettableListenableFuture<String>();
				Executors.newSingleThreadExecutor().execute(new Runnable() {

					@Override
					public void run() {
						try {
							latch.await(10, TimeUnit.SECONDS);
							switch (whichTest) {
								case 0:
									try {
										future.set("reply");
									}
									catch (Exception e) {
										failedCallbackException = e;
										noOutputChannelLatch.countDown();
									}
									break;
								case 1:
									future.setException(new RuntimeException("foo"));
									break;
								case 2:
									try {
										future.setException(new MessagingException(requestMessage));
									}
									catch (Exception e) {
										failedCallbackException = e;
										exceptionLatch.countDown();
									}
							}
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}

				});
				return future;
			}

		};
		this.handler.setAsyncReplySupported(true);
		this.handler.setOutputChannel(this.output);
		this.latch = new CountDownLatch(1);
	}

	@Test
	public void testGoodResult() {
		this.whichTest = 0;
		this.handler.handleMessage(new GenericMessage<String>("foo"));
		assertNull(this.output.receive(0));
		this.latch.countDown();
		Message<?> received = this.output.receive(10000);
		assertNotNull(received);
		assertEquals("reply", received.getPayload());
		assertNull(this.failedCallbackException);
	}

	@Test
	public void testGoodResultWithReplyChannelHeader() {
		this.whichTest = 0;
		this.handler.setOutputChannel(null);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();
		this.handler.handleMessage(message);
		assertNull(replyChannel.receive(0));
		this.latch.countDown();
		Message<?> received = replyChannel.receive(10000);
		assertNotNull(received);
		assertEquals("reply", received.getPayload());
		assertNull(this.failedCallbackException);
	}

	@Test
	public void testGoodResultWithNoReplyChannelHeaderNoOutput() throws Exception {
		this.whichTest = 0;
		this.handler.setOutputChannel(null);
		this.handler.handleMessage(new GenericMessage<String>("foo"));
		assertNull(this.output.receive(0));
		this.latch.countDown();
		assertTrue(this.noOutputChannelLatch.await(10, TimeUnit.SECONDS));
		assertNotNull(this.failedCallbackException);
		assertThat(this.failedCallbackException.getMessage(), containsString("or replyChannel header"));
	}

	@Test
	public void testRuntimeException() {
		QueueChannel errorChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setErrorChannel(errorChannel)
				.build();
		this.handler.handleMessage(message);
		assertNull(this.output.receive(0));
		this.whichTest = 1;
		this.latch.countDown();
		Message<?> received = errorChannel.receive(10000);
		assertNotNull(received);
		assertThat(received.getPayload(), instanceOf(MessageHandlingException.class));
		assertEquals("foo", ((Throwable) received.getPayload()).getCause().getMessage());
		assertSame(message, ((MessagingException) received.getPayload()).getFailedMessage());
		assertNull(this.failedCallbackException);
	}

	@Test
	public void testMessagingException() {
		QueueChannel errorChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setErrorChannel(errorChannel)
				.build();
		this.handler.handleMessage(message);
		assertNull(this.output.receive(0));
		this.whichTest = 2;
		this.latch.countDown();
		Message<?> received = errorChannel.receive(10000);
		assertNotNull(received);
		assertThat(received.getPayload(), instanceOf(MessagingException.class));
		assertSame(message, ((MessagingException) received.getPayload()).getFailedMessage());
		assertNull(this.failedCallbackException);
	}

	@Test
	public void testMessagingExceptionNoErrorChannel() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.build();
		this.handler.handleMessage(message);
		assertNull(this.output.receive(0));
		this.whichTest = 2;
		this.latch.countDown();
		assertTrue(this.exceptionLatch.await(10, TimeUnit.SECONDS));
		assertNotNull(this.failedCallbackException);
		assertThat(this.failedCallbackException.getMessage(), containsString("no 'errorChannel' header"));
	}

	@Test
	public void testGateway() throws Exception {
		this.whichTest = 0;
		GatewayProxyFactoryBean gpfb = new GatewayProxyFactoryBean(Foo.class);
		gpfb.setBeanFactory(mock(BeanFactory.class));
		DirectChannel input = new DirectChannel();
		gpfb.setDefaultRequestChannel(input);
		gpfb.setDefaultReplyTimeout(10000L);
		gpfb.afterPropertiesSet();
		Foo foo = (Foo) gpfb.getObject();
		this.handler.setOutputChannel(null);
		EventDrivenConsumer consumer = new EventDrivenConsumer(input, this.handler);
		consumer.afterPropertiesSet();
		consumer.start();
		this.latch.countDown();
		String result = foo.exchange("foo");
		assertEquals("reply", result);
	}

	@Test
	public void testGatewayWithException() throws Exception {
		this.whichTest = 0;
		GatewayProxyFactoryBean gpfb = new GatewayProxyFactoryBean(Foo.class);
		gpfb.setBeanFactory(mock(BeanFactory.class));
		DirectChannel input = new DirectChannel();
		gpfb.setDefaultRequestChannel(input);
		gpfb.setDefaultReplyTimeout(10000L);
		gpfb.afterPropertiesSet();
		Foo foo = (Foo) gpfb.getObject();
		this.handler.setOutputChannel(null);
		EventDrivenConsumer consumer = new EventDrivenConsumer(input, this.handler);
		consumer.afterPropertiesSet();
		consumer.start();
		this.latch.countDown();
		try {
			foo.exchange("foo");
		}
		catch (MessagingException e) {
			assertThat(e.getClass().getSimpleName(), equalTo("RuntimeException"));
			assertThat(e.getMessage(), equalTo("foo"));
		}
	}

	private interface Foo {

		String exchange(String payload);
	}

}
