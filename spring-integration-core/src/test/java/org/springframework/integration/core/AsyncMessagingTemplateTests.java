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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class AsyncMessagingTemplateTests {

	@Test
	public void asyncSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultChannel(channel);
		long start = System.currentTimeMillis();
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 200);
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
		assertTrue(elapsed >= 200);
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
		assertTrue(elapsed >= 200);
		assertEquals("TEST", result.get().getPayload());
	}

	@Test
	public void asyncConvertSendAndReceiveWithDefaultChannel() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultChannel(channel);
		long start = System.currentTimeMillis();
		Future<String> result = template.asyncConvertSendAndReceive("test");
		assertNotNull(result.get());
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 200);
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
		assertTrue(elapsed >= 200);
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
		assertTrue(elapsed >= 200);
		assertEquals("TEST", result.get());
	}

	@Test(expected = TimeoutException.class)
	public void timeoutException() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(200));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultChannel(channel);
		Future<Message<?>> result = template.asyncSendAndReceive(MessageBuilder.withPayload("test").build());
		result.get(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MessagingException.class)
	public void executionException() throws Throwable {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new EchoHandler(-1));
		AsyncMessagingTemplate template = new AsyncMessagingTemplate();
		template.setDefaultChannel(channel);
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
		template.setDefaultChannel(channel);
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
			return requestMessage.getPayload().toString().toUpperCase();
		}
	}

}
