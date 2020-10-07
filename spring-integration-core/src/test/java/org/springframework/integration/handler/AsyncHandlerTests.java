/*
 * Copyright 2016-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class AsyncHandlerTests {

	private final QueueChannel output = new QueueChannel();

	private AbstractReplyProducingMessageHandler handler;

	private volatile CountDownLatch latch;

	private volatile int whichTest;

	private volatile Exception failedCallbackException;

	private volatile String failedCallbackMessage;

	private volatile CountDownLatch exceptionLatch = new CountDownLatch(1);

	private ExecutorService executor;

	@BeforeEach
	public void setup() {
		this.executor = Executors.newSingleThreadExecutor();
		this.handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				final SettableListenableFuture<String> future = new SettableListenableFuture<>();
				AsyncHandlerTests.this.executor.execute(() -> {
					try {
						latch.await(10, TimeUnit.SECONDS);
						switch (whichTest) {
							case 0:
								future.set("reply");
								break;
							case 1:
								future.setException(new RuntimeException("foo"));
								break;
							case 2:
								future.setException(new MessagingException(requestMessage));
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				return future;
			}

		};
		this.handler.setAsync(true);
		this.handler.setOutputChannel(this.output);
		this.handler.setBeanFactory(mock(BeanFactory.class));
		this.latch = new CountDownLatch(1);
		LogAccessor logAccessor = TestUtils.getPropertyValue(this.handler, "logger", LogAccessor.class);
		Log log = spy(logAccessor.getLog());
		new DirectFieldAccessor(logAccessor).setPropertyValue("log", log);
		doAnswer(invocation -> {
			failedCallbackMessage = invocation.getArgument(0).toString();
			failedCallbackException = invocation.getArgument(1);
			exceptionLatch.countDown();
			return null;
		}).when(log).error(any(), any(Throwable.class));
	}

	@AfterEach
	public void tearDown() {
		this.executor.shutdownNow();
	}

	@Test
	public void testGoodResult() {
		this.whichTest = 0;
		this.handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(this.output.receive(0)).isNull();
		this.latch.countDown();
		Message<?> received = this.output.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("reply");
		assertThat(this.failedCallbackException).isNull();
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
		assertThat(replyChannel.receive(0)).isNull();
		this.latch.countDown();
		Message<?> received = replyChannel.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("reply");
		assertThat(this.failedCallbackException).isNull();
	}

	@Test
	public void testGoodResultWithNoReplyChannelHeaderNoOutput() {
		this.whichTest = 0;
		this.handler.setOutputChannel(null);
		QueueChannel errorChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo").setErrorChannel(errorChannel).build();
		this.handler.handleMessage(message);
		assertThat(this.output.receive(0)).isNull();
		this.latch.countDown();
		Message<?> errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.getPayload()).isInstanceOf(DestinationResolutionException.class);
		assertThat(((Throwable) errorMessage.getPayload()).getMessage())
				.isEqualTo("no output-channel or replyChannel header available");
		assertThat(((MessagingException) errorMessage.getPayload()).getFailedMessage()).isNull();
		assertThat(this.failedCallbackException).isNotNull();
		assertThat(this.failedCallbackException.getMessage()).contains("or replyChannel header");
	}

	@Test
	public void testRuntimeException() {
		QueueChannel errorChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setErrorChannel(errorChannel)
				.build();
		this.handler.handleMessage(message);
		assertThat(this.output.receive(0)).isNull();
		this.whichTest = 1;
		this.latch.countDown();
		Message<?> received = errorChannel.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(MessageHandlingException.class);
		assertThat(((Throwable) received.getPayload()).getCause().getMessage()).isEqualTo("foo");
		assertThat(((MessagingException) received.getPayload()).getFailedMessage()).isSameAs(message);
		assertThat(this.failedCallbackException).isNull();
	}

	@Test
	public void testMessagingException() {
		QueueChannel errorChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo")
				.setErrorChannel(errorChannel)
				.build();
		this.handler.handleMessage(message);
		assertThat(this.output.receive(0)).isNull();
		this.whichTest = 2;
		this.latch.countDown();
		Message<?> received = errorChannel.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) received.getPayload()).getFailedMessage()).isSameAs(message);
		assertThat(this.failedCallbackException).isNull();
	}

	@Test
	public void testMessagingExceptionNoErrorChannel() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo")
				.build();
		this.handler.handleMessage(message);
		assertThat(this.output.receive(0)).isNull();
		this.whichTest = 2;
		this.latch.countDown();
		assertThat(this.exceptionLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.failedCallbackException).isNotNull();
		assertThat(this.failedCallbackMessage).contains("no 'errorChannel' header");
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
		assertThat(result).isEqualTo("reply");
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
			assertThat(e.getClass().getSimpleName()).isEqualTo("RuntimeException");
			assertThat(e.getMessage()).isEqualTo("foo");
		}
	}

	private interface Foo {

		String exchange(String payload);

	}

}
