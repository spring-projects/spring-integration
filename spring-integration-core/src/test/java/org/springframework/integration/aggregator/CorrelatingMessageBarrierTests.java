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

package org.springframework.integration.aggregator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 */
@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageBarrierTests {

	private CorrelatingMessageBarrier barrier;

	@Mock
	private CorrelationStrategy correlationStrategy;

	@Mock
	private ReleaseStrategy releaseStrategy;

	@Before
	public void initializeBarrier() {
		barrier = new CorrelatingMessageBarrier();
		barrier.setCorrelationStrategy(correlationStrategy);
		barrier.setReleaseStrategy(releaseStrategy);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn("foo");
		when(releaseStrategy.canRelease(isA(MessageGroup.class))).thenReturn(true);
	}

	@Test
	public void shouldPassMessage() {
		Message<Object> message = testMessage();
		barrier.handleMessage(message);
		assertThat(barrier.receive()).isEqualTo(message);
	}

	@Test
	public void shouldRemoveKeyWithoutLockingOnEmptyQueue() throws InterruptedException {
		Message<Object> message = testMessage();
		Message<Object> message2 = testMessage();
		barrier.handleMessage(message);
		verify(correlationStrategy).getCorrelationKey(message);
		assertThat(barrier.receive()).isNotNull();
		barrier.handleMessage(message2);
		assertThat(barrier.receive()).isNotNull();
		assertThat(barrier.receive()).isNull();
	}

	@Test(timeout = 10000)
	public void shouldNotDropMessageOrBlockSendingThread() {
		OneMessagePerKeyReleaseStrategy trackingReleaseStrategy = new OneMessagePerKeyReleaseStrategy();
		barrier.setReleaseStrategy(trackingReleaseStrategy);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch sent = new CountDownLatch(200);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		for (int i = 0; i < 200; i++) {
			sendAsynchronously(barrier, testMessage(), start, sent, exec);
		}
		start.countDown();

		try {
			sent.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertThat((barrier.receive())).isNotNull();
		for (int i = 0; i < 199; i++) {
			trackingReleaseStrategy.release("foo");
			assertThat((barrier.receive())).isNotNull();
		}
		exec.shutdownNow();
	}

	private void sendAsynchronously(final MessageHandler handler, final Message<Object> message,
			final CountDownLatch start, final CountDownLatch sent, ExecutorService exec) {
		exec.execute(() -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			handler.handleMessage(message);
			sent.countDown();
		});
	}

	private Message<Object> testMessage() {
		return MessageBuilder.withPayload((Object) "payload").build();
	}

	/**
	 * ReleaseStrategy that emulates the use case described in INT-1068
	 */
	private static class OneMessagePerKeyReleaseStrategy implements ReleaseStrategy {

		private final ConcurrentMap<Object, Semaphore> keyLocks = new ConcurrentHashMap<Object, Semaphore>();

		OneMessagePerKeyReleaseStrategy() {
			super();
		}

		@Override
		public boolean canRelease(MessageGroup messageGroup) {
			Object correlationKey = messageGroup.getGroupId();
			Semaphore lock = lockForKey(correlationKey);
			return lock.tryAcquire();
		}

		private Semaphore lockForKey(Object correlationKey) {
			Semaphore semaphore = keyLocks.get(correlationKey);
			if (semaphore == null) {
				keyLocks.putIfAbsent(correlationKey, new Semaphore(1));
				semaphore = keyLocks.get(correlationKey);
			}
			return semaphore;
		}

		public void release(String correlationKey) {
			Semaphore lock = keyLocks.get(correlationKey);
			if (lock != null) {
				lock.release();
			}
		}

		@SuppressWarnings("unused")
		public void releaseAll() {
			for (Semaphore semaphore : keyLocks.values()) {
				semaphore.release();
			}
		}

	}

}
