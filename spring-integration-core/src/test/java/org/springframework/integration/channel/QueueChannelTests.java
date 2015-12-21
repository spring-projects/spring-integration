/*
 * Copyright 2002-2015 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.integration.selector.UnexpiredMessageSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class QueueChannelTests {

	@Test
	public void testSimpleSendAndReceive() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		final QueueChannel channel = new QueueChannel();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive();
				if (message != null) {
					messageReceived.set(true);
					latch.countDown();
				}
			}
		}).start();
		assertFalse(messageReceived.get());
		channel.send(new GenericMessage<String>("testing"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(messageReceived.get());
	}

	@Test
	public void testImmediateReceive() throws Exception {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final QueueChannel channel = new QueueChannel();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
		Runnable receiveTask1 = new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive(0);
				if (message != null) {
					messageReceived.set(true);
				}
				latch1.countDown();
			}
		};
		Runnable sendTask = new Runnable() {
			@Override
			public void run() {
				channel.send(new GenericMessage<String>("testing"));
			}
		};
		singleThreadExecutor.execute(receiveTask1);
		latch1.await();
		singleThreadExecutor.execute(sendTask);
		assertFalse(messageReceived.get());
		Runnable receiveTask2 = new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive(0);
				if (message != null) {
					messageReceived.set(true);
				}
				latch2.countDown();
			}
		};
		singleThreadExecutor.execute(receiveTask2);
		latch2.await();
		assertTrue(messageReceived.get());
	}

	@Test
	public void testBlockingReceiveWithNoTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel();
		final AtomicBoolean receiveInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive();
				receiveInterrupted.set(true);
				assertTrue(message == null);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(receiveInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(receiveInterrupted.get());
	}

	@Test
	public void testBlockingReceiveWithTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel();
		final AtomicBoolean receiveInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive(10000);
				receiveInterrupted.set(true);
				assertTrue(message == null);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(receiveInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(receiveInterrupted.get());
	}

	@Test
	public void testImmediateSend() {
		QueueChannel channel = new QueueChannel(3);
		boolean result1 = channel.send(new GenericMessage<String>("test-1"));
		assertTrue(result1);
		boolean result2 = channel.send(new GenericMessage<String>("test-2"), 100);
		assertTrue(result2);
		boolean result3 = channel.send(new GenericMessage<String>("test-3"), 0);
		assertTrue(result3);
		boolean result4 = channel.send(new GenericMessage<String>("test-4"), 0);
		assertFalse(result4);
	}

	@Test
	public void testBlockingSendWithNoTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel(1);
		boolean result1 = channel.send(new GenericMessage<String>("test-1"));
		assertTrue(result1);
		final AtomicBoolean sendInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				channel.send(new GenericMessage<String>("test-2"));
				sendInterrupted.set(true);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(sendInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(sendInterrupted.get());
	}

	@Test
	public void testBlockingSendWithTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel(1);
		boolean result1 = channel.send(new GenericMessage<String>("test-1"));
		assertTrue(result1);
		final AtomicBoolean sendInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				channel.send(new GenericMessage<String>("test-2"), 10000);
				sendInterrupted.set(true);
				latch.countDown();
			}
		});
		t.start();
		assertFalse(sendInterrupted.get());
		t.interrupt();
		latch.await();
		assertTrue(sendInterrupted.get());
	}

	@Test
	public void testClear() {
		QueueChannel channel = new QueueChannel(2);
		GenericMessage<String> message1 = new GenericMessage<String>("test1");
		GenericMessage<String> message2 = new GenericMessage<String>("test2");
		GenericMessage<String> message3 = new GenericMessage<String>("test3");
		assertTrue(channel.send(message1));
		assertTrue(channel.send(message2));
		assertFalse(channel.send(message3, 0));
		List<Message<?>> clearedMessages = channel.clear();
		assertNotNull(clearedMessages);
		assertEquals(2, clearedMessages.size());
		assertTrue(channel.send(message3));
	}

	@Test
	public void testClearEmptyChannel() {
		QueueChannel channel = new QueueChannel();
		List<Message<?>> clearedMessages = channel.clear();
		assertNotNull(clearedMessages);
		assertEquals(0, clearedMessages.size());
	}

	@Test
	public void testPurge() {
		QueueChannel channel = new QueueChannel(2);
		long minute = 60 * 1000;
		long time = System.currentTimeMillis();
		long past = time - minute;
		long future = time + minute;
		Message<String> expiredMessage = MessageBuilder.withPayload("test1")
				.setExpirationDate(past).build();
		Message<String> unexpiredMessage = MessageBuilder.withPayload("test2")
				.setExpirationDate(future).build();
		assertTrue(channel.send(expiredMessage, 0));
		assertTrue(channel.send(unexpiredMessage, 0));
		assertFalse(channel.send(new GenericMessage<String>("atCapacity"), 0));
		List<Message<?>> purgedMessages = channel.purge(new UnexpiredMessageSelector());
		assertNotNull(purgedMessages);
		assertEquals(1, purgedMessages.size());
		assertTrue(channel.send(new GenericMessage<String>("roomAvailable"), 0));
	}

	@Rule
	public final TemporaryFolder tempFolder = new TemporaryFolder();

	/*TODO: No Reactor Chronicle artifact
	@Test
	public void testReactorPersistentQueue() throws InterruptedException, IOException {
		final AtomicBoolean messageReceived = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		PersistentQueue<Message<?>> queue = new PersistentQueueSpec<Message<?>>()
				.codec(new JavaSerializationCodec<Message<?>>())
				.basePath(this.tempFolder.getRoot().getAbsolutePath())
				.get();

		final QueueChannel channel = new QueueChannel(queue);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive();
				if (message != null) {
					messageReceived.set(true);
					latch.countDown();
				}
			}
		}).start();
		assertFalse(messageReceived.get());
		channel.send(new GenericMessage<String>("testing"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(messageReceived.get());

		final CountDownLatch latch1 = new CountDownLatch(2);

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Message<?> message = channel.receive(100);
					if (message != null) {
						latch1.countDown();
						if (latch1.getCount() == 0) {
							break;
						}
					}
				}
			}
		});
		thread.start();

		Thread.sleep(200);
		channel.send(new GenericMessage<String>("testing"));
		channel.send(new GenericMessage<String>("testing"));
		assertTrue(latch1.await(1000, TimeUnit.MILLISECONDS));

		final AtomicBoolean receiveInterrupted = new AtomicBoolean(false);
		final CountDownLatch latch2 = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive(10000);
				receiveInterrupted.set(true);
				assertTrue(message == null);
				latch2.countDown();
			}
		});
		t.start();
		assertFalse(receiveInterrupted.get());
		t.interrupt();
		latch2.await();
		assertTrue(receiveInterrupted.get());

		receiveInterrupted.set(false);
		final CountDownLatch latch3 = new CountDownLatch(1);
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive();
				receiveInterrupted.set(true);
				assertTrue(message == null);
				latch3.countDown();
			}
		});
		t.start();
		assertFalse(receiveInterrupted.get());
		t.interrupt();
		latch3.await();
		assertTrue(receiveInterrupted.get());

		GenericMessage<String> message1 = new GenericMessage<String>("test1");
		GenericMessage<String> message2 = new GenericMessage<String>("test2");
		assertTrue(channel.send(message1));
		assertTrue(channel.send(message2));
		List<Message<?>> clearedMessages = channel.clear();
		assertNotNull(clearedMessages);
		assertEquals(2, clearedMessages.size());

		clearedMessages = channel.clear();
		assertNotNull(clearedMessages);
		assertEquals(0, clearedMessages.size());

		// Test on artificial infinite wait
		// channel.receive();

		// Distributed scenario
		final CountDownLatch latch4 = new CountDownLatch(1);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> message = channel.receive();
				if (message != null) {
					latch4.countDown();
				}
			}
		}).start();
		queue.add(new GenericMessage<String>("foo"));
		assertTrue(latch4.await(1000, TimeUnit.MILLISECONDS));
	}
*/
}
