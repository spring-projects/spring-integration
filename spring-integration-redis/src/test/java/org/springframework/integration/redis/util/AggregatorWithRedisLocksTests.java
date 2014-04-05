/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.redis.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AggregatorWithRedisLocksTests extends RedisAvailableTests {

	@Autowired
	private LatchingReleaseStrategy releaseStrategy;

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel in2;

	@Autowired
	private PollableChannel out;

	private volatile Exception exception;

	private RedisTemplate<String, ?> template;

	@Before
	@After
	public void setup() {
		this.template = this.createTemplate();
		Set<String> keys = template.keys("aggregatorWithRedisLocksTests:*");
		for (String key : keys) {
			template.delete(key);
		}
	}

	@Test
	@RedisAvailable
	public void testLockSingleGroup() throws Exception {
		this.releaseStrategy.reset(1);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 1));
		assertTrue(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS));
		assertEquals(1, this.template.keys("aggregatorWithRedisLocksTests:*").size());
		this.releaseStrategy.latch1.countDown();
		assertNotNull(this.out.receive(10000));
		assertEquals(1, this.releaseStrategy.maxCallers.get());
		this.assertNoLocksAfterTest();
		assertNull("Unexpected exception:" + (this.exception != null ? this.exception.toString() : ""), this.exception);
	}

	@Test
	@RedisAvailable
	public void testLockThreeGroups() throws Exception {
		this.releaseStrategy.reset(3);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 2));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 2));
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 3));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 3));
		assertTrue(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS));
		assertEquals(3, this.template.keys("aggregatorWithRedisLocksTests:*").size());
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		assertNotNull(this.out.receive(10000));
		assertNotNull(this.out.receive(10000));
		assertNotNull(this.out.receive(10000));
		assertEquals(3, this.releaseStrategy.maxCallers.get());
		this.assertNoLocksAfterTest();
		assertNull("Unexpected exception:" + (this.exception != null ? this.exception.toString() : ""), this.exception);
	}

	@Test
	@RedisAvailable
	public void testDistributedAggregator() throws Exception {
		this.releaseStrategy.reset(1);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					in2.send(new GenericMessage<String>("bar", stubHeaders(2, 2, 1)));
				}
				catch (Exception e) {
					e.printStackTrace();
					exception = e;
				}
			}
		});
		assertTrue(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS));
		assertEquals(1, this.template.keys("aggregatorWithRedisLocksTests:*").size());
		this.releaseStrategy.latch1.countDown();
		assertNotNull(this.out.receive(10000));
		assertEquals(1, this.releaseStrategy.maxCallers.get());
		this.assertNoLocksAfterTest();
		assertNull("Unexpected exception:" + (this.exception != null ? this.exception.toString() : ""), this.exception);
	}

	private void assertNoLocksAfterTest() throws Exception {
		int n = 0;
		while (n++ < 100 && this.template.keys("aggregatorWithRedisLocksTests:*").size() > 0) {
			Thread.sleep(100);
		}
		assertEquals(0, this.template.keys("aggregatorWithRedisLocksTests:*").size());
	}

	private Runnable asyncSend(final String payload, final int sequence, final int correlation) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					in.send(new GenericMessage<String>(payload, stubHeaders(sequence, 2, correlation)));
				}
				catch (Exception e) {
					e.printStackTrace();
					exception = e;
				}
			}
		};
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

	private RedisTemplate<String, ?> createTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(this.getConnectionFactoryForTest());
		template.setKeySerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}

	public static class LatchingReleaseStrategy implements ReleaseStrategy {

		private volatile CountDownLatch latch1;

		private volatile CountDownLatch latch2;

		private volatile AtomicInteger callers;

		private volatile AtomicInteger maxCallers;

		@Override
		public boolean canRelease(MessageGroup group) {
			synchronized(this) {
				this.callers.incrementAndGet();
				this.maxCallers.set(Math.max(this.maxCallers.get(), this.callers.get()));
			}
			this.latch2.countDown();
			try {
				this.latch1.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			this.callers.decrementAndGet();
			return group.size() > 1;
		}

		public void reset(int expectedConcurrency) {
			this.latch1 = new CountDownLatch(expectedConcurrency);
			this.latch2 = new CountDownLatch(expectedConcurrency);
			this.callers = new AtomicInteger();
			this.maxCallers = new AtomicInteger();
		}
	}
}
