/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.store;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MessageGroupQueueTests {

	private static final Log logger = LogFactory.getLog(MessageGroupQueueTests.class);

	@Test
	public void testPutAndPoll() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<>("foo"));
		Message<?> result = queue.poll(100, TimeUnit.MILLISECONDS);
		assertThat(result).isNotNull();
	}

	@Test
	public void testPollTimeout() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		Message<?> result = queue.poll(1, TimeUnit.MILLISECONDS);
		assertThat(result).isNull();
	}

	@Test
	public void testPollEmpty() throws Exception {
		MessageGroupQueue queue = spy(new MessageGroupQueue(new SimpleMessageStore(), "FOO"));
		CountDownLatch latch1 = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		willAnswer(i -> {
			latch1.countDown();
			return first.getAndSet(false) ? null : i.callRealMethod();
		}).given(queue).doPoll();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		CountDownLatch latch2 = new CountDownLatch(1);
		exec.execute(() -> {
			try {
				Message<?> result = queue.poll(10, TimeUnit.SECONDS);
				if (result != null) {
					latch2.countDown();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		queue.put(new GenericMessage<>("foo"));
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		exec.shutdownNow();
	}

	@Test
	public void testSize() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<>("foo"));
		assertThat(queue.size()).isEqualTo(1);
		queue.poll(100, TimeUnit.MILLISECONDS);
		assertThat(queue.size()).isEqualTo(0);
	}

	@Test
	public void testCapacityAfterExpiry() throws Exception {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO", 2);
		queue.put(new GenericMessage<>("foo"));
		assertThat(queue.remainingCapacity()).isEqualTo(1);
		queue.put(new GenericMessage<>("bar"));
		assertThat(queue.remainingCapacity()).isEqualTo(0);
		Message<?> result = queue.poll(100, TimeUnit.MILLISECONDS);
		assertThat(result).isNotNull();
		assertThat(queue.remainingCapacity()).isEqualTo(1);
	}

	@Test
	public void testCapacityExceeded() throws Exception {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO", 1);
		queue.put(new GenericMessage<>("foo"));
		assertThat(queue.offer(new GenericMessage<>("bar"), 100, TimeUnit.MILLISECONDS)).isFalse();
	}

	@Test
	public void testPutAndTake() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<>("foo"));
		Message<?> result = queue.take();
		assertThat(result).isNotNull();
	}

	@Test
	public void testConcurrentAccess() throws Exception {
		doTestConcurrentAccess(50, 20, new HashSet<>());
	}

	@Test
	public void testConcurrentAccessUniqueResults() throws Exception {
		doTestConcurrentAccess(50, 20, null);
	}

	private void doTestConcurrentAccess(int concurrency, final int maxPerTask, final Set<String> set) throws Exception {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		final MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO");
		ExecutorService executorService = Executors.newCachedThreadPool();
		CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executorService);

		for (int i = 0; i < concurrency; i++) {

			final int big = i;

			completionService.submit(() -> {
				boolean result = true;
				for (int j = 0; j < maxPerTask; j++) {
					result &= queue.add(new GenericMessage<>("count=" + big + ":" + j));
					if (!result) {
						logger.warn("Failed to add");
					}
				}
				return result;
			});

			completionService.submit(() -> {
				boolean result = true;
				for (int j = 0; j < maxPerTask; j++) {
					@SuppressWarnings("unchecked")
					Message<String> item = (Message<String>) queue.poll(10, TimeUnit.SECONDS);
					result &= item != null;
					if (!result) {
						logger.warn("Failed to poll");
					}
					else if (set != null) {
						synchronized (set) {
							set.add(item.getPayload());
						}
					}
				}
				return result;
			});

			messageGroupStore.expireMessageGroups(-10000);

		}

		for (int j = 0; j < 2 * concurrency; j++) {
			assertThat(completionService.take().get()).isTrue();
		}

		if (set != null) {
			// Ensure all items polled are unique
			assertThat(set.size()).isEqualTo(concurrency * maxPerTask);
		}

		assertThat(queue.size()).isEqualTo(0);
		messageGroupStore.expireMessageGroups(-10000);
		assertThat(queue.remainingCapacity()).isEqualTo(Integer.MAX_VALUE);

		executorService.shutdown();
	}

}
