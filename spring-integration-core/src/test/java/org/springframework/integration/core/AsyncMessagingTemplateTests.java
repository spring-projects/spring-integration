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

package org.springframework.integration.core;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class AsyncMessagingTemplateTests {

	@Test
	public void asyncSendWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Message<?> message = MessageBuilder.withPayload("test").build();
		Future<?> future = template.asyncSend(message);
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result).isEqualTo(message);
	}

	@Test
	public void asyncSendWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Message<?> message = MessageBuilder.withPayload("test").build();
		Future<?> future = template.asyncSend(channel, message);
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result).isEqualTo(message);
	}

	@Test
	public void asyncSendWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		Message<?> message = MessageBuilder.withPayload("test").build();
		Future<?> future = template.asyncSend("testChannel", message);
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result).isEqualTo(message);
		context.close();
	}

	@Test
	public void asyncSendWithTimeoutException() {
		QueueChannel channel = new QueueChannel(1);
		channel.send(MessageBuilder.withPayload("blocker").build());
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncSend(channel, MessageBuilder.withPayload("test").build());
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void asyncConvertAndSendWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<?> future = template.asyncConvertAndSend("test");
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void asyncConvertAndSendWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> future = template.asyncConvertAndSend(channel, "test");
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void asyncConvertAndSendWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		Future<?> future = template.asyncConvertAndSend("testChannel", "test");
		assertThat(future.get(10000, TimeUnit.MILLISECONDS)).isNull();
		Message<?> result = channel.receive(0);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void asyncConvertAndSendWithTimeoutException() {
		QueueChannel channel = new QueueChannel(1);
		channel.send(MessageBuilder.withPayload("blocker").build());
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncConvertAndSend(channel, "test");
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void asyncReceiveWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncReceive();
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(100000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;
		assertThat(result.get().getPayload()).isEqualTo("test");
		assertThat(elapsed).isGreaterThanOrEqualTo(200);
	}

	@Test
	public void asyncReceiveWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<Message<?>> result = template.asyncReceive(channel);
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(10000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;
		assertThat(result.get().getPayload()).isEqualTo("test");
		assertThat(elapsed).isGreaterThanOrEqualTo(200);
	}

	@Test
	public void asyncReceiveWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		Future<Message<?>> result = template.asyncReceive("testChannel");
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(10000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get().getPayload()).isEqualTo("test");
	}

	@Test
	public void asyncReceiveWithTimeoutException() {
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<Message<?>> result = template.asyncReceive(new QueueChannel());
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void asyncReceiveAndConvertWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<?> result = template.asyncReceiveAndConvert();
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(10000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;
		assertThat(result.get()).isEqualTo("test");

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
	}

	@Test
	public void asyncReceiveAndConvertWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncReceiveAndConvert(channel);
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(10000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;
		assertThat(result.get()).isEqualTo("test");

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
	}

	@Test
	public void asyncReceiveAndConvertWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", QueueChannel.class);
		context.refresh();
		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		Future<?> result = template.asyncReceiveAndConvert("testChannel");
		long start = System.currentTimeMillis();
		sendMessageAfterDelay(channel, new GenericMessage<>("test"), 200);
		assertThat(result.get(10000, TimeUnit.MILLISECONDS)).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("test");
	}

	@Test
	public void asyncReceiveAndConvertWithTimeoutException() {
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncReceiveAndConvert(new QueueChannel());
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void asyncSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
	}

	@Test
	public void asyncSendAndReceiveWithExplicitChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get().getPayload()).isEqualTo("TEST");
	}

	@Test
	public void asyncSendAndReceiveWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", DirectChannel.class);
		context.refresh();
		DirectChannel channel = context.getBean("testChannel", DirectChannel.class);
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive("testChannel", new GenericMessage<>("test"));
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get().getPayload()).isEqualTo("TEST");
	}

	@Test
	public void asyncConvertSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive("test");
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("TEST");
	}

	@Test
	public void asyncConvertSendAndReceiveWithExplicitChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(channel, "test");
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("TEST");
	}

	@Test
	public void asyncConvertSendAndReceiveWithResolvedChannel() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", DirectChannel.class);
		context.refresh();
		DirectChannel channel = context.getBean("testChannel", DirectChannel.class);
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive("testChannel", "test");
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("TEST");
	}

	@Test
	public void asyncConvertSendAndReceiveWithDefaultChannelAndMessagePostProcessor() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(123, new TestMessagePostProcessor());
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("123-bar");
	}

	@Test
	public void asyncConvertSendAndReceiveWithExplicitChannelAndMessagePostProcessor() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(channel, "test", new TestMessagePostProcessor());
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("TEST-bar");
	}

	@Test
	public void asyncConvertSendAndReceiveWithResolvedChannelAndMessagePostProcessor() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("testChannel", DirectChannel.class);
		context.refresh();
		DirectChannel channel = context.getBean("testChannel", DirectChannel.class);
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setBeanFactory(context);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive("testChannel", "test", new TestMessagePostProcessor());
		assertThat(result.get()).isNotNull();
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isGreaterThanOrEqualTo(200);
		assertThat(result.get()).isEqualTo("TEST-bar");
	}

	@Test
	public void timeoutException() {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(10000));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		assertThatExceptionOfType(TimeoutException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void executionException() {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(-1));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS))
				.withCauseInstanceOf(MessagingException.class);
	}

	@Test
	public void cancellationException() throws Throwable {
		DirectChannel channel = new DirectChannel();
		EchoHandler handler = new EchoHandler(10000);
		channel.subscribe(handler);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());

		Thread.sleep(200);
		result.cancel(true);

		assertThatExceptionOfType(CancellationException.class)
				.isThrownBy(result::get);

		await().until(() -> handler.interrupted);
	}

	private static void sendMessageAfterDelay(MessageChannel channel, GenericMessage<String> message, int delay) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			try {
				Thread.sleep(delay);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			channel.send(message);
		});
		executorService.shutdown();
	}

	private static class EchoHandler extends AbstractReplyProducingMessageHandler {

		private final long delay;

		private final boolean shouldFail;

		private volatile boolean interrupted;

		private EchoHandler(long delay) {
			this.delay = delay;
			this.shouldFail = (this.delay < 0);
		}

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			if (this.shouldFail) {
				throw new MessagingException("intentional test failure in " + AsyncMessagingTemplateTests.class.getName());
			}
			try {
				Thread.sleep(this.delay);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				this.interrupted = true;
				return null;
			}
			String result = requestMessage.getPayload().toString().toUpperCase();
			String header = requestMessage.getHeaders().get("foo", String.class);
			return (header != null) ? result + "-" + header : result;
		}

	}

	private static class TestMessagePostProcessor implements MessagePostProcessor {

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			return MessageBuilder.fromMessage(message).setHeader("foo", "bar").build();
		}

	}

}
