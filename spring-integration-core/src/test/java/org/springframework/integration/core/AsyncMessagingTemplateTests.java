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

package org.springframework.integration.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class AsyncMessagingTemplateTests {

	// TODO: changed from 0 because of recurrent failure: is this right?
	private long safety = 100;

	@Test
	public void asyncSendWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Message<?> message = MessageBuilder.withPayload("test").build();
		Future<?> future = template.asyncSend(message);
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals(message, result);
	}

	@Test
	public void asyncSendWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Message<?> message = MessageBuilder.withPayload("test").build();
		Future<?> future = template.asyncSend(channel, message);
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals(message, result);
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
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals(message, result);
	}

	@Test(expected = TimeoutException.class)
	public void asyncSendWithTimeoutException() throws Exception {
		QueueChannel channel = new QueueChannel(1);
		channel.send(MessageBuilder.withPayload("blocker").build());
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncSend(channel, MessageBuilder.withPayload("test").build());
		result.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void asyncConvertAndSendWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<?> future = template.asyncConvertAndSend("test");
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void asyncConvertAndSendWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> future = template.asyncConvertAndSend(channel, "test");
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals("test", result.getPayload());
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
		assertNull(future.get(1000, TimeUnit.MILLISECONDS));
		Message<?> result = channel.receive(0);
		assertEquals("test", result.getPayload());
	}

	@Test(expected = TimeoutException.class)
	public void asyncConvertAndSendWithTimeoutException() throws Exception {
		QueueChannel channel = new QueueChannel(1);
		channel.send(MessageBuilder.withPayload("blocker").build());
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncConvertAndSend(channel, "test");
		result.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void asyncReceiveWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncReceive();
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertEquals("test", result.get().getPayload());
		assertTrue(elapsed >= 200-safety);
	}

	@Test
	public void asyncReceiveWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<Message<?>> result = template.asyncReceive(channel);
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertEquals("test", result.get().getPayload());
		assertTrue(elapsed >= 200-safety);
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
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("test", result.get().getPayload());
	}

	@Test(expected = TimeoutException.class)
	public void asyncReceiveWithTimeoutException() throws Exception {
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<Message<?>> result = template.asyncReceive(new QueueChannel());
		result.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void asyncReceiveAndConvertWithDefaultChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<?> result = template.asyncReceiveAndConvert();
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertEquals("test", result.get());

		assertTrue(elapsed >= 200-safety);
	}

	@Test
	public void asyncReceiveAndConvertWithExplicitChannel() throws Exception {
		QueueChannel channel = new QueueChannel();
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncReceiveAndConvert(channel);
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertEquals("test", result.get());

		assertTrue(elapsed >= 200-safety);
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
		sendMessageAfterDelay(channel, new GenericMessage<String>("test"), 200);
		long start = System.currentTimeMillis();
		assertNotNull(result.get(1000, TimeUnit.MILLISECONDS));
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("test", result.get());
	}

	@Test(expected = TimeoutException.class)
	public void asyncReceiveAndConvertWithTimeoutException() throws Exception {
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		Future<?> result = template.asyncReceiveAndConvert(new QueueChannel());
		result.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void asyncSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
	}

	@Test
	public void asyncSendAndReceiveWithExplicitChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive(channel, MessageBuilder.withPayload("test").build());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST", result.get().getPayload());
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
		Future<Message<?>> result = template.asyncSendAndReceive("testChannel", MessageBuilder.withPayload("test").build());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST", result.get().getPayload());
	}

	@Test
	public void asyncConvertSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive("test");
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST", result.get());
	}

	@Test
	public void asyncConvertSendAndReceiveWithExplicitChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(channel, "test");
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST", result.get());
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
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST", result.get());
	}

	@Test
	public void asyncConvertSendAndReceiveWithDefaultChannelAndMessagePostProcessor() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(new Integer(123), new TestMessagePostProcessor());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("123-bar", result.get());
	}

	@Test
	public void asyncConvertSendAndReceiveWithExplicitChannelAndMessagePostProcessor() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive(channel, "test", new TestMessagePostProcessor());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST-bar", result.get());
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
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;

		assertTrue(elapsed >= 200-safety);
		assertEquals("TEST-bar", result.get());
	}

	@Test(expected = TimeoutException.class)
	public void timeoutException() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		result.get(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MessagingException.class)
	public void executionException() throws Throwable {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(-1));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		try {
			result.get(10, TimeUnit.MILLISECONDS);
			fail();
		}
		catch (ExecutionException e) {
			throw e.getCause();
		}
	}

	@Test(expected = CancellationException.class)
	public void cancellationException() throws Throwable {
		DirectChannel channel = new DirectChannel();
		EchoHandler handler = new EchoHandler(10000);
		channel.subscribe(handler);
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultDestination(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		try {
			Thread.sleep(200);
			result.cancel(true);
			result.get();
			fail();
		}
		catch (ExecutionException e) {
			Assert.isTrue(handler.interrupted, "handler should have been interrupted");
			throw e.getCause();
		}
	}


	private static void sendMessageAfterDelay(final MessageChannel channel, final GenericMessage<String> message, final int delay) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				channel.send(message);
			}
		});
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

		public Message<?> postProcessMessage(Message<?> message) {
			return MessageBuilder.fromMessage(message).setHeader("foo", "bar").build();
		}
	}

}
