/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.store.MessageGroup;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Iwein Fuld
 */
@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageBarrierTest {

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
		Message message = testMessage();
		barrier.handleMessage(message);
		assertThat(barrier.receive(), is(message));
	}

	@Test
	public void shouldRemoveKeyWithoutLockingOnEmptyQueue() throws InterruptedException {
		Message message = testMessage();
		Message message2 = testMessage();
		barrier.handleMessage(message);
		verify(correlationStrategy).getCorrelationKey(message);
		assertThat(barrier.receive(), is(notNullValue()));
		barrier.handleMessage(message2);
		assertThat(barrier.receive(), is(notNullValue()));
		assertThat(barrier.receive(), is(nullValue()));
	}

	@Test(timeout = 10000)
	public void shouldNotDropMessageOrBlockSendingThread() {
		OneMessagePerKeyReleaseStrategy trackingReleaseStrategy = new OneMessagePerKeyReleaseStrategy();
		barrier.setReleaseStrategy(trackingReleaseStrategy);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch sent = new CountDownLatch(200);
		for (int i = 0; i < 200; i++) {
			sendAsynchronously(barrier, testMessage(), start, sent);
		}
		start.countDown();

		try {
			sent.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertThat((barrier.receive()), is(notNullValue()));
		for (int i = 0; i < 199; i++) {
			trackingReleaseStrategy.release("foo");
			assertThat((barrier.receive()), is(notNullValue()));
		}
	}

	private void sendAsynchronously(final MessageHandler handler, final Message<?> message, final CountDownLatch start, final CountDownLatch sent) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				handler.handleMessage(message);
				sent.countDown();
			}
		});

	}

	private Message testMessage() {
		return MessageBuilder.withPayload("payload").build();
	}


	/**
	 * ReleaseStrategy that emulates the use case described in INT-1068
	 */
	private static class OneMessagePerKeyReleaseStrategy implements ReleaseStrategy {
		private final ConcurrentMap<Object, Semaphore> keyLocks = new ConcurrentHashMap<Object, Semaphore>();

		public boolean canRelease(MessageGroup messageGroup) {
			System.out.println("Trying to release group: " + messageGroup + "\n to thread: " + Thread.currentThread());
			Object correlationKey = messageGroup.getCorrelationKey();
			Semaphore lock = lockForKey(correlationKey);
			System.out.println(Thread.currentThread() + " got lock: " + lock);
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

		public void releaseAll() {
			for (Semaphore semaphore : keyLocks.values()) {
				semaphore.release();
			}
		}
	}
}
