/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;

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
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public class MessageGroupQueueTests {

	static final Log logger = LogFactory.getLog(MessageGroupQueueTests.class);

	@Test
	public void testPutAndPoll() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<String>("foo"));
		Message<?> result = queue.poll(100, TimeUnit.MILLISECONDS);
		assertNotNull(result);
	}

	@Test
	public void testPollTimeout() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		Message<?> result = queue.poll(1, TimeUnit.MILLISECONDS);
		assertNull(result);
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
				Message<?> result = queue.poll(100, TimeUnit.MILLISECONDS);
				if (result != null) {
					latch2.countDown();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		queue.put(new GenericMessage<String>("foo"));
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		exec.shutdownNow();
	}

	@Test
	public void testSize() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<String>("foo"));
		assertEquals(1, queue.size());
		queue.poll(100, TimeUnit.MILLISECONDS);
		assertEquals(0, queue.size());
	}

	@Test
	public void testCapacityAfterExpiry() throws Exception {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO", 2);
		queue.put(new GenericMessage<String>("foo"));
		assertEquals(1, queue.remainingCapacity());
		queue.put(new GenericMessage<String>("bar"));
		assertEquals(0, queue.remainingCapacity());
		Message<?> result = queue.poll(100, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals(1, queue.remainingCapacity());
	}

	@Test
	public void testCapacityExceeded() throws Exception {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO", 1);
		queue.put(new GenericMessage<String>("foo"));
		assertFalse(queue.offer(new GenericMessage<String>("bar"), 100, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testPutAndTake() throws Exception {
		MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), "FOO");
		queue.put(new GenericMessage<String>("foo"));
		Message<?> result = queue.take();
		assertNotNull(result);
	}

	@Test
	public void testConcurrentAccess() throws Exception {
		doTestConcurrentAccess(50, 20, new HashSet<String>());
	}

	@Test
	public void testConcurrentAccessUniqueResults() throws Exception {
		doTestConcurrentAccess(50, 20, null);
	}

	private void doTestConcurrentAccess(int concurrency, final int maxPerTask, final Set<String> set) throws Exception {

		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		final MessageGroupQueue queue = new MessageGroupQueue(messageGroupStore, "FOO");
		ExecutorService executorService = Executors.newCachedThreadPool();
		CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executorService);

		for (int i = 0; i < concurrency; i++) {

			final int big = i;

			completionService.submit(() -> {
				boolean result = true;
				for (int j = 0; j < maxPerTask; j++) {
					result &= queue.add(new GenericMessage<String>("count=" + big + ":" + j));
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
			assertTrue(completionService.take().get());
		}

		if (set != null) {
			// Ensure all items polled are unique
			assertEquals(concurrency * maxPerTask, set.size());
		}

		assertEquals(0, queue.size());
		messageGroupStore.expireMessageGroups(-10000);
		assertEquals(Integer.MAX_VALUE, queue.remainingCapacity());

		executorService.shutdown();

	}

}
