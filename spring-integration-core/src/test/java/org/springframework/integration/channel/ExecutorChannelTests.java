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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * @author Mark Fisher
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
		channel.send(new GenericMessage<String>("test"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertNotNull(handler.thread);
		assertFalse(Thread.currentThread().equals(handler.thread));
		assertEquals("test-1", handler.thread.getName());
	}

	@Test
	public void roundRobinLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(
				Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-")));
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
			channel.send(new GenericMessage<String>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertNotNull(handler1.thread);
		assertFalse(Thread.currentThread().equals(handler1.thread));
		assertTrue(handler1.thread.getName().startsWith("test-"));
		assertNotNull(handler2.thread);
		assertFalse(Thread.currentThread().equals(handler2.thread));
		assertTrue(handler2.thread.getName().startsWith("test-"));
		assertNotNull(handler3.thread);
		assertFalse(Thread.currentThread().equals(handler3.thread));
		assertTrue(handler3.thread.getName().startsWith("test-"));
		assertEquals(4, handler1.count.get());
		assertEquals(4, handler2.count.get());
		assertEquals(3, handler3.count.get());
	}

	@Test
	public void verifyFailoverWithLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(
				Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-")));
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
			channel.send(new GenericMessage<String>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertNotNull(handler1.thread);
		assertFalse(Thread.currentThread().equals(handler1.thread));
		assertTrue(handler1.thread.getName().startsWith("test-"));
		assertNotNull(handler2.thread);
		assertFalse(Thread.currentThread().equals(handler2.thread));
		assertTrue(handler2.thread.getName().startsWith("test-"));
		assertNotNull(handler3.thread);
		assertFalse(Thread.currentThread().equals(handler3.thread));
		assertTrue(handler3.thread.getName().startsWith("test-"));
		assertEquals(0, handler2.count.get());
		assertEquals(4, handler1.count.get());
		assertEquals(7, handler3.count.get());
	}

	@Test
	public void verifyFailoverWithoutLoadBalancing() throws Exception {
		int numberOfMessages = 11;
		ConcurrentTaskExecutor taskExecutor = new ConcurrentTaskExecutor(
				Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("test-")));
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
			channel.send(new GenericMessage<String>("test-" + i));
		}
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertNotNull(handler1.thread);
		assertFalse(Thread.currentThread().equals(handler1.thread));
		assertTrue(handler1.thread.getName().startsWith("test-"));
		assertNotNull(handler2.thread);
		assertFalse(Thread.currentThread().equals(handler2.thread));
		assertTrue(handler2.thread.getName().startsWith("test-"));
		assertNull(handler3.thread);
		assertEquals(0, handler1.count.get());
		assertEquals(0, handler3.count.get());
		assertEquals(numberOfMessages, handler2.count.get());
	}


	private static class TestHandler implements MessageHandler {

		private final CountDownLatch latch;

		private final AtomicInteger count = new AtomicInteger();

		private volatile Thread thread;

		private volatile boolean shouldFail;

		public TestHandler(CountDownLatch latch) {
			this.latch = latch;
		}

		public void handleMessage(Message<?> message) {
			this.thread = Thread.currentThread();
			if (this.shouldFail) {
				throw new RuntimeException("intentional test failure");
			}
			this.count.incrementAndGet();
			this.latch.countDown();
		}
	}

}
