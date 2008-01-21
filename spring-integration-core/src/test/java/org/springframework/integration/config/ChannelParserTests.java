/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.DefaultMessageDispatcher;
import org.springframework.integration.dispatcher.DispatcherPolicy;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ChannelParserTests {

	@Test
	public void testChannelWithCapacity() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("capacityChannel");
		for (int i = 0; i < 10; i++) {
			boolean result = channel.send(new GenericMessage<String>(1, "test"), 10);
			assertTrue(result);
		}
		assertFalse(channel.send(new GenericMessage<String>(1, "test"), 3));
	}

	@Test
	public void testPointToPointChannelByDefault() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("pointToPointChannelByDefault");
		channel.send(new StringMessage("test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		AtomicInteger counter = new AtomicInteger();
		CountDownLatch latch = new CountDownLatch(1);
		TestHandler handler1 = new TestHandler(counter, latch);
		TestHandler handler2 = new TestHandler(counter, latch);
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, counter.get());
	}

	@Test
	public void testPointToPointChannelExplicitlyConfigured() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("pointToPointChannelExplicitlyConfigured");
		channel.send(new StringMessage("test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		AtomicInteger counter = new AtomicInteger();
		CountDownLatch latch = new CountDownLatch(1);
		TestHandler handler1 = new TestHandler(counter, latch);
		TestHandler handler2 = new TestHandler(counter, latch);
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, counter.get());
	}

	@Test
	public void testPublishSubscribeChannel() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("publishSubscribeChannel");
		channel.send(new StringMessage("test"));
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		AtomicInteger counter = new AtomicInteger();
		CountDownLatch latch = new CountDownLatch(2);
		TestHandler handler1 = new TestHandler(counter, latch);
		TestHandler handler2 = new TestHandler(counter, latch);
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		dispatcher.start();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(2, counter.get());
	}

	@Test
	public void testDefaultDispatcherPolicy() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("pointToPointChannelByDefault");
		DispatcherPolicy dispatcherPolicy = channel.getDispatcherPolicy();
		assertFalse(dispatcherPolicy.isPublishSubscribe());
		assertEquals(DispatcherPolicy.DEFAULT_MAX_MESSAGES_PER_TASK, dispatcherPolicy.getMaxMessagesPerTask());
		assertEquals(DispatcherPolicy.DEFAULT_RECEIVE_TIMEOUT, dispatcherPolicy.getReceiveTimeout());
		assertEquals(DispatcherPolicy.DEFAULT_REJECTION_LIMIT, dispatcherPolicy.getRejectionLimit());
		assertEquals(DispatcherPolicy.DEFAULT_RETRY_INTERVAL, dispatcherPolicy.getRetryInterval());
		assertTrue(dispatcherPolicy.getShouldFailOnRejectionLimit());
	}

	@Test
	public void testDispatcherPolicyConfiguration() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"channelParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channelWithDispatcherPolicy");
		DispatcherPolicy dispatcherPolicy = channel.getDispatcherPolicy();
		assertTrue(dispatcherPolicy.isPublishSubscribe());
		assertEquals(7, dispatcherPolicy.getMaxMessagesPerTask());
		assertEquals(77, dispatcherPolicy.getReceiveTimeout());
		assertEquals(777, dispatcherPolicy.getRejectionLimit());
		assertEquals(7777, dispatcherPolicy.getRetryInterval());
		assertFalse(dispatcherPolicy.getShouldFailOnRejectionLimit());
	}


	private static class TestHandler implements MessageHandler {

		private AtomicInteger counter;

		private CountDownLatch latch;

		TestHandler(AtomicInteger counter, CountDownLatch latch) {
			this.counter = counter;
			this.latch = latch;
		}

		public Message<?> handle(Message<?> message) {
			this.counter.incrementAndGet();
			this.latch.countDown();
			return null;
		}
	}

}
