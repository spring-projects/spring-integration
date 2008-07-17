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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class ReplyMessageCorrelatorTests {

	@Test
	public void testReceiversPrecedeReply() throws InterruptedException {
		final ReplyMessageCorrelator correlator = new ReplyMessageCorrelator(10);
		final AtomicInteger replyCounter = new AtomicInteger();
		CountDownLatch latch = startReceivers(correlator, replyCounter, 5, 500);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setCorrelationId("123").build();
		correlator.handle(message);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, replyCounter.get());
	}

	@Test
	public void testReplyPrecedeReceivers() throws InterruptedException {
		final ReplyMessageCorrelator correlator = new ReplyMessageCorrelator(10);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setCorrelationId("123").build();
		correlator.handle(message);
		final AtomicInteger replyCounter = new AtomicInteger();
		CountDownLatch latch = startReceivers(correlator, replyCounter, 5, 50);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals(1, replyCounter.get());
	}


	private static CountDownLatch startReceivers(final ReplyMessageCorrelator correlator,
			final AtomicInteger replyCounter, int numReceivers, final long timeout) {
		final CountDownLatch latch = new CountDownLatch(numReceivers);
		Executor executor = Executors.newFixedThreadPool(numReceivers);
		for (int i = 0; i < numReceivers; i++) {
			executor.execute(new Runnable() {
				public void run() {
					Message<?> reply = correlator.getReply("123", timeout);
					if (reply != null) {
						replyCounter.incrementAndGet();
					}
					latch.countDown();
				}
			});
		}
		return latch;
	}

}
