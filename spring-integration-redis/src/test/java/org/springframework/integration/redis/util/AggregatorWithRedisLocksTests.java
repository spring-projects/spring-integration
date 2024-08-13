/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.redis.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class AggregatorWithRedisLocksTests implements RedisContainerTest {

	@Autowired
	private LatchingReleaseStrategy releaseStrategy;

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel in2;

	@Autowired
	private PollableChannel out;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	private volatile Exception exception;

	private RedisTemplate<String, ?> template;

	@BeforeEach
	@AfterEach
	public void setup() {
		this.template = this.createTemplate();
		Set<String> keys = template.keys("aggregatorWithRedisLocksTests:*");
		for (String key : keys) {
			template.delete(key);
		}
	}

	@Test
	void testLockSingleGroup() throws Exception {
		this.releaseStrategy.reset(1);
		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.execute(asyncSend("foo", 1, 1));
		executorService.execute(asyncSend("bar", 2, 1));
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.template.keys("aggregatorWithRedisLocksTests:*")).hasSize(1);
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(1);
		this.assertNoLocksAfterTest();
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
		executorService.shutdown();
	}

	@Test
	void testLockThreeGroups() throws Exception {
		this.releaseStrategy.reset(3);
		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.execute(asyncSend("foo", 1, 1));
		executorService.execute(asyncSend("bar", 2, 1));
		executorService.execute(asyncSend("foo", 1, 2));
		executorService.execute(asyncSend("bar", 2, 2));
		executorService.execute(asyncSend("foo", 1, 3));
		executorService.execute(asyncSend("bar", 2, 3));
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.template.keys("aggregatorWithRedisLocksTests:*")).hasSize(3);
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(3);
		this.assertNoLocksAfterTest();
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
		executorService.shutdown();
	}

	@RepeatedTest(10)
	void testDistributedAggregator() throws Exception {
		this.releaseStrategy.reset(1);
		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.execute(asyncSend("foo", 1, 1));
		executorService.execute(() -> {
			try {
				in2.send(new GenericMessage<>("bar", stubHeaders(2, 2, 1)));
			}
			catch (Exception e) {
				exception = e;
			}
		});
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.template.keys("aggregatorWithRedisLocksTests:*")).hasSize(1);
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(1);
		this.assertNoLocksAfterTest();
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
		executorService.shutdown();
	}

	private void assertNoLocksAfterTest() throws Exception {
		int n = 0;
		while (n++ < 100 && this.template.keys("aggregatorWithRedisLocksTests:*").size() > 0) {
			Thread.sleep(100);
		}
		assertThat(this.template.keys("aggregatorWithRedisLocksTests:*")).isEmpty();
	}

	private Runnable asyncSend(final String payload, final int sequence, final int correlation) {
		return () -> {
			try {
				in.send(new GenericMessage<>(payload, stubHeaders(sequence, 2, correlation)));
			}
			catch (Exception e) {
				exception = e;
			}
		};
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		var headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

	private RedisTemplate<String, ?> createTemplate() {
		var template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(redisConnectionFactory);
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
			synchronized (this) {
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
