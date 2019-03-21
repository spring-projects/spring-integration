/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
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
		final CountDownLatch latch = new CountDownLatch(1);
		final QueueChannel channel = new QueueChannel();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive();
			if (message != null) {
				latch.countDown();
			}
		});
		channel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testSimpleSendAndReceiveNonBlockingQueue() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final QueueChannel channel = new QueueChannel(new ArrayDeque<>());
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive();
			if (message != null) {
				latch.countDown();
			}
		});
		channel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testSimpleSendAndReceiveNonBlockingQueueWithTimeout() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final QueueChannel channel = new QueueChannel(new ArrayDeque<>());
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive(1);
			if (message != null) {
				latch.countDown();
			}
		});
		channel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testImmediateReceive() throws Exception {
		final AtomicBoolean messageNull = new AtomicBoolean(false);
		final QueueChannel channel = new QueueChannel();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
		Runnable receiveTask1 = () -> {
			Message<?> message = channel.receive(0);
			messageNull.set(message == null);
			latch1.countDown();
		};
		Runnable sendTask = () -> channel.send(new GenericMessage<>("testing"));
		singleThreadExecutor.execute(receiveTask1);
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		singleThreadExecutor.execute(sendTask);
		Runnable receiveTask2 = () -> {
			Message<?> message = channel.receive(0);
			if (message != null) {
				latch2.countDown();
			}
		};
		singleThreadExecutor.execute(receiveTask2);
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		singleThreadExecutor.shutdownNow();
	}

	@Test
	public void testBlockingReceiveWithNoTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel();
		final AtomicBoolean messageNull = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive();
			messageNull.set(message == null);
			latch.countDown();
		});
		exec.shutdownNow();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messageNull.get()).isTrue();
	}

	@Test
	public void testBlockingReceiveWithTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel();
		final AtomicBoolean messageNull = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive(10000);
			messageNull.set(message == null);
			latch.countDown();
		});
		exec.shutdownNow();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(messageNull.get()).isTrue();
	}

	@Test
	public void testBlockingReceiveWithTimeoutEmptyThenSend() throws Exception {
		Queue<Message<?>> queue = spy(new ArrayDeque<>());
		CountDownLatch pollLatch = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		willAnswer(i -> {
			pollLatch.countDown();
			return first.getAndSet(false) ? null : i.callRealMethod();
		}).given(queue).poll();
		final QueueChannel channel = new QueueChannel(queue);
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive(10000);
			if (message != null) {
				latch.countDown();
			}
		});
		assertThat(pollLatch.await(10, TimeUnit.SECONDS)).isTrue();
		channel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testBlockingReceiveNoTimeoutEmptyThenSend() throws Exception {
		Queue<Message<?>> queue = spy(new ArrayDeque<>());
		CountDownLatch pollLatch = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		willAnswer(i -> {
			pollLatch.countDown();
			return first.getAndSet(false) ? null : i.callRealMethod();
		}).given(queue).poll();
		final QueueChannel channel = new QueueChannel(queue);
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			Message<?> message = channel.receive();
			if (message != null) {
				latch.countDown();
			}
		});
		assertThat(pollLatch.await(10, TimeUnit.SECONDS)).isTrue();
		channel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testImmediateSend() {
		QueueChannel channel = new QueueChannel(3);
		boolean result1 = channel.send(new GenericMessage<>("test-1"));
		assertThat(result1).isTrue();
		boolean result2 = channel.send(new GenericMessage<>("test-2"), 100);
		assertThat(result2).isTrue();
		boolean result3 = channel.send(new GenericMessage<>("test-3"), 0);
		assertThat(result3).isTrue();
		boolean result4 = channel.send(new GenericMessage<>("test-4"), 0);
		assertThat(result4).isFalse();
	}

	@Test
	public void testBlockingSendWithNoTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel(1);
		boolean result1 = channel.send(new GenericMessage<>("test-1"));
		assertThat(result1).isTrue();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			channel.send(new GenericMessage<>("test-2"));
			latch.countDown();
		});
		exec.shutdownNow();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testBlockingSendWithTimeout() throws Exception {
		final QueueChannel channel = new QueueChannel(1);
		boolean result1 = channel.send(new GenericMessage<>("test-1"));
		assertThat(result1).isTrue();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			channel.send(new GenericMessage<>("test-2"), 10000);
			latch.countDown();
		});
		exec.shutdownNow();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testClear() {
		QueueChannel channel = new QueueChannel(2);
		GenericMessage<String> message1 = new GenericMessage<>("test1");
		GenericMessage<String> message2 = new GenericMessage<>("test2");
		GenericMessage<String> message3 = new GenericMessage<>("test3");
		assertThat(channel.send(message1)).isTrue();
		assertThat(channel.send(message2)).isTrue();
		assertThat(channel.send(message3, 0)).isFalse();
		List<Message<?>> clearedMessages = channel.clear();
		assertThat(clearedMessages).isNotNull();
		assertThat(clearedMessages.size()).isEqualTo(2);
		assertThat(channel.send(message3)).isTrue();
	}

	@Test
	public void testClearEmptyChannel() {
		QueueChannel channel = new QueueChannel();
		List<Message<?>> clearedMessages = channel.clear();
		assertThat(clearedMessages).isNotNull();
		assertThat(clearedMessages.size()).isEqualTo(0);
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
		assertThat(channel.send(expiredMessage, 0)).isTrue();
		assertThat(channel.send(unexpiredMessage, 0)).isTrue();
		assertThat(channel.send(new GenericMessage<>("atCapacity"), 0)).isFalse();
		List<Message<?>> purgedMessages = channel.purge(new UnexpiredMessageSelector());
		assertThat(purgedMessages).isNotNull();
		assertThat(purgedMessages.size()).isEqualTo(1);
		assertThat(channel.send(new GenericMessage<>("roomAvailable"), 0)).isTrue();
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
