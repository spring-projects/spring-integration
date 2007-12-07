/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Mark Fisher
 */
public class EventDrivenConsumerTests {

	@Test
	public void testDynamicConcurrency() throws Exception {
		int messagesToSend = 200;
		int concurrency = 1;
		int maxConcurrency = 10;
		final AtomicInteger counter = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(messagesToSend);
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		final AtomicInteger maxActive = new AtomicInteger(0);
		final AtomicInteger activeSum = new AtomicInteger(0);
		executor.setCorePoolSize(concurrency);
		executor.setMaxPoolSize(maxConcurrency);
		executor.setQueueCapacity(0);
		PointToPointChannel channel = new PointToPointChannel();
		MessageEndpoint endpoint = new MessageEndpoint() {
			public void messageReceived(Message message) {
				counter.incrementAndGet();
				latch.countDown();
				activeSum.set(activeSum.addAndGet(executor.getActiveCount()));
				maxActive.set(Math.max(executor.getActiveCount(), maxActive.get()));
			}

			public ConsumerType getConsumerType() {
				return ConsumerType.EVENT_DRIVEN;
			}
		};
		EventDrivenConsumer consumer = new EventDrivenConsumer(channel, endpoint);
		consumer.setExecutor(executor);
		consumer.setConcurrency(concurrency);
		consumer.setMaxConcurrency(maxConcurrency);
		consumer.setIdleTaskExecutionLimit(1);
		consumer.setMaxMessagesPerTask(1);
		consumer.setReceiveTimeout(100);
		consumer.initialize();
		for (int i = 0; i < messagesToSend - 110; i++) {
			channel.send(new DocumentMessage(1, "fast-1." + (i+1)));
		}
		int activeCountAfterFirstBurst = executor.getActiveCount();
		for (int i = 0; i < 10; i++) {
			channel.send(new DocumentMessage(1, "slow-1." + (i+1)));
			Thread.sleep(50);
		}
		int activeCountAfterSlowDown = executor.getActiveCount();
		for (int i = 0; i < 100; i++) {
			channel.send(new DocumentMessage(1, "fast-2." + (i+1)));
		}
		int activeCountAfterLastBurst = executor.getActiveCount();
		latch.await(10, TimeUnit.SECONDS);
		int averageActive = activeSum.get() / messagesToSend;
		assertTrue(activeCountAfterSlowDown < activeCountAfterFirstBurst);
		assertTrue(activeCountAfterLastBurst > activeCountAfterSlowDown);
		assertEquals(messagesToSend, counter.get());
		assertEquals(maxConcurrency, maxActive.get());
		assertTrue(averageActive > concurrency);
		assertTrue(averageActive < maxActive.get());
	}

}
