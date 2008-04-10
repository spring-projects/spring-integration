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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.adapter.PollableSource;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SynchronousChannelTests {

	private static final String HANDLER_THREAD = "handler-thread";


	@Test
	public void testSend() {
		SynchronousChannel channel = new SynchronousChannel();
		channel.addHandler(new ThreadNameSettingTestHandler());
		StringMessage message = new StringMessage("test");
		assertTrue(channel.send(message));
		String handlerThreadName = message.getHeader().getProperty(HANDLER_THREAD);
		assertEquals(Thread.currentThread().getName(), handlerThreadName);
	}

	@Test
	public void testSendAndReceiveWithNoHandler() {
		SynchronousChannel channel = new SynchronousChannel();
		StringMessage message = new StringMessage("test");
		assertNull(channel.receive());
		assertTrue(channel.send(message));
		Message<?> response = channel.receive();
		assertNotNull(response);
		assertEquals(response, message);
		assertNull(channel.receive());
	}

	@Test
	public void testSendAndClearWithNoHandler() {
		SynchronousChannel channel = new SynchronousChannel();
		StringMessage message1 = new StringMessage("test1");
		StringMessage message2 = new StringMessage("test2");
		assertNull(channel.receive());
		assertTrue(channel.send(message1));
		assertTrue(channel.send(message2));
		List<Message<?>> clearedMessages = channel.clear();
		assertEquals(2, clearedMessages.size());
		assertEquals(message1, clearedMessages.get(0));
		assertEquals(message2, clearedMessages.get(1));
		assertNull(channel.receive());
	}

	@Test
	public void testSendInSeparateThread() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final SynchronousChannel channel = new SynchronousChannel();
		channel.addHandler(new ThreadNameSettingTestHandler(latch));
		final StringMessage message = new StringMessage("test");
		new Thread(new Runnable() {
			public void run() {
				channel.send(message);
			}
		}, "test-thread").start();
		latch.await(1000, TimeUnit.MILLISECONDS);
		String handlerThreadName = message.getHeader().getProperty(HANDLER_THREAD);
		assertEquals("test-thread", handlerThreadName);
	}

	@Test
	public void testReceive() {
		SynchronousChannel channel = new SynchronousChannel(new PollableSource<String>() {
			public Message<String> poll() {
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
		SynchronousChannel channel = new SynchronousChannel(new MessageReturningTestSource("foo"));
		Message<?> message = channel.receive();
		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("foo", message.getPayload());
		String handlerThreadName = message.getHeader().getProperty(HANDLER_THREAD);
		assertEquals(Thread.currentThread().getName(), handlerThreadName);
	}

	@Test
	public void testReceiveInSeparateThread() throws InterruptedException {
		final SynchronousChannel channel = new SynchronousChannel(new MessageReturningTestSource("foo"));
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
		String handlerThreadName = message.getHeader().getProperty(HANDLER_THREAD);
		assertEquals("test-thread", handlerThreadName);
	}

	@Test
	public void testReceiveWithNoSource() {
		SynchronousChannel channel = new SynchronousChannel();
		assertNull(channel.receive());
	}


	private static class ThreadNameSettingTestHandler implements MessageHandler {

		private final CountDownLatch latch;


		ThreadNameSettingTestHandler() {
			this(null);
		}

		ThreadNameSettingTestHandler(CountDownLatch latch) {
			this.latch = latch;
		}

		public Message<?> handle(Message<?> message) {
			message.getHeader().setProperty(HANDLER_THREAD, Thread.currentThread().getName());
			if (this.latch != null) {
				this.latch.countDown();
			}
			return null;
		}
	}


	private static class MessageReturningTestSource implements PollableSource<String> {

		private final String messageText;


		MessageReturningTestSource(String messageText) {
			this.messageText = messageText;
		}

		public StringMessage poll() {
			StringMessage message = new StringMessage(messageText);
			message.getHeader().setProperty(HANDLER_THREAD, Thread.currentThread().getName());
			return message;
		}
	}

}
