/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DirectChannelTests {

	private static final String HANDLER_THREAD = "handler-thread";


	@Test
	public void testSend() {
		DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget();
		channel.subscribe(target);
		StringMessage message = new StringMessage("test");
		assertTrue(channel.send(message));
		assertEquals(Thread.currentThread().getName(), target.threadName);
	}

	@Test
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final DirectChannel channel = new DirectChannel();
		ThreadNameExtractingTestTarget target = new ThreadNameExtractingTestTarget(latch);
		channel.subscribe(target);
		final StringMessage message = new StringMessage("test");
		new Thread(new Runnable() {
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals("test-thread", target.threadName);
	}

	@Test
	public void testReceive() {
		DirectChannel channel = new DirectChannel(new MessageSource<String>() {
			public Message<String> receive() {
				return new StringMessage("foo");
			}
		});
		Message<?> message = channel.receive();
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testReceiveWithMessageResult() {
		DirectChannel channel = new DirectChannel(new MessageReturningTestSource("foo"));
		Message<?> message = channel.receive();
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("foo", message.getPayload());
		String handlerThreadName = message.getHeaders().get(HANDLER_THREAD, String.class);
		assertEquals(Thread.currentThread().getName(), handlerThreadName);
	}

	@Test
	public void testReceiveInSeparateThread() throws InterruptedException {
		final DirectChannel channel = new DirectChannel(new MessageReturningTestSource("foo"));
		final SynchronousQueue<Message<?>> messageHolder = new SynchronousQueue<Message<?>>();
		new Thread(new Runnable() {
			public void run() {
				Message<?> message = channel.receive();
				assertNotNull(message);
				try {
					messageHolder.put(message);
				}
				catch (InterruptedException e) {
					// will fail after timeout below
				}
			}
		}, "test-thread").start();
		Message<?> message = messageHolder.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("foo", message.getPayload());
		String handlerThreadName = message.getHeaders().get(HANDLER_THREAD, String.class);
		assertEquals("test-thread", handlerThreadName);
	}

	@Test
	public void testReceiveWithNoSource() {
		DirectChannel channel = new DirectChannel();
		assertNull(channel.receive());
	}


	private static class ThreadNameExtractingTestTarget implements MessageTarget {

		private String threadName;

		private final CountDownLatch latch;


		ThreadNameExtractingTestTarget() {
			this(null);
		}

		ThreadNameExtractingTestTarget(CountDownLatch latch) {
			this.latch = latch;
		}

		public boolean send(Message<?> message) {
			this.threadName = Thread.currentThread().getName();
			if (this.latch != null) {
				this.latch.countDown();
			}
			return true;
		}
	}


	private static class MessageReturningTestSource implements MessageSource<String> {

		private final String messageText;


		MessageReturningTestSource(String messageText) {
			this.messageText = messageText;
		}

		public Message<String> receive() {
			return MessageBuilder.fromPayload(messageText)
					.setHeader(HANDLER_THREAD, Thread.currentThread().getName()).build();
		}
	}

}
