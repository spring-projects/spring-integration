/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.redis.leader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.integration.test.condition.LogLevels;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 * @author Artem Vozhdayenko
 *
 * @since 4.3.9
 */
@LogLevels(categories = "org.springframework.integration.redis.leader")
class RedisLockRegistryLeaderInitiatorTests implements RedisContainerTest {
	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	void testDistributedLeaderElection() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, "LeaderInitiator");
		registry.expireUnusedOlderThan(-1);
		CountDownLatch granted = new CountDownLatch(1);
		CountingPublisher countingPublisher = new CountingPublisher(granted);
		List<LockRegistryLeaderInitiator> initiators = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			LockRegistryLeaderInitiator initiator =
					new LockRegistryLeaderInitiator(registry, new DefaultCandidate("foo:" + i, "bar"));
			initiator.setTaskExecutor(new SimpleAsyncTaskExecutor("lock-leadership-" + i + "-"));
			initiator.setLeaderEventPublisher(countingPublisher);
			initiators.add(initiator);
		}

		for (LockRegistryLeaderInitiator initiator : initiators) {
			initiator.start();
		}

		assertThat(granted.await(60, TimeUnit.SECONDS)).isTrue();

		LockRegistryLeaderInitiator initiator1 = countingPublisher.initiator;

		LockRegistryLeaderInitiator initiator2 = null;

		for (LockRegistryLeaderInitiator initiator : initiators) {
			if (initiator != initiator1) {
				initiator2 = initiator;
				break;
			}
		}

		assertThat(initiator2).isNotNull();

		assertThat(initiator1.getContext().isLeader()).isTrue();
		assertThat(initiator2.getContext().isLeader()).isFalse();

		final CountDownLatch granted1 = new CountDownLatch(1);
		final CountDownLatch granted2 = new CountDownLatch(1);
		CountDownLatch revoked1 = new CountDownLatch(1);
		CountDownLatch revoked2 = new CountDownLatch(1);
		CountDownLatch acquireLockFailed1 = new CountDownLatch(1);
		CountDownLatch acquireLockFailed2 = new CountDownLatch(1);

		initiator1.setLeaderEventPublisher(new CountingPublisher(granted1, revoked1, acquireLockFailed1));

		initiator2.setLeaderEventPublisher(new CountingPublisher(granted2, revoked2, acquireLockFailed2));

		// It's hard to see round-robin election, so let's make the yielding initiator to sleep long before restarting
		initiator1.setBusyWaitMillis(10000);

		Thread.sleep(100);

		initiator1.getContext().yield();

		assertThat(revoked1.await(60, TimeUnit.SECONDS)).isTrue();
		assertThat(granted2.await(60, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator2.getContext().isLeader()).isTrue();
		assertThat(initiator1.getContext().isLeader()).isFalse();

		initiator1.setBusyWaitMillis(LockRegistryLeaderInitiator.DEFAULT_BUSY_WAIT_TIME);
		initiator2.setBusyWaitMillis(10000);

		Thread.sleep(100);

		initiator2.getContext().yield();

		assertThat(revoked2.await(60, TimeUnit.SECONDS)).isTrue();
		assertThat(granted1.await(60, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator1.getContext().isLeader()).isTrue();
		assertThat(initiator2.getContext().isLeader()).isFalse();

		initiator2.stop();

		CountDownLatch revoked11 = new CountDownLatch(1);
		initiator1.setLeaderEventPublisher(new CountingPublisher(new CountDownLatch(1), revoked11,
				new CountDownLatch(1)));

		initiator1.stop();

		assertThat(revoked11.await(60, TimeUnit.SECONDS)).isTrue();
		assertThat(initiator1.getContext().isLeader()).isFalse();
	}

	private static class CountingPublisher implements LeaderEventPublisher {

		private final CountDownLatch granted;

		private final CountDownLatch revoked;

		private volatile LockRegistryLeaderInitiator initiator;

		private final CountDownLatch acquireLockFailed;

		CountingPublisher(CountDownLatch granted, CountDownLatch revoked, CountDownLatch acquireLockFailed) {
			this.granted = granted;
			this.revoked = revoked;
			this.acquireLockFailed = acquireLockFailed;
		}

		CountingPublisher(CountDownLatch granted) {
			this(granted, new CountDownLatch(1), new CountDownLatch(1));
		}

		@Override
		public void publishOnRevoked(Object source, Context context, String role) {
			this.revoked.countDown();
		}

		@Override
		public void publishOnFailedToAcquire(Object source, Context context, String role) {
			this.acquireLockFailed.countDown();
		}

		@Override
		public void publishOnGranted(Object source, Context context, String role) {
			this.initiator = (LockRegistryLeaderInitiator) source;
			this.granted.countDown();
		}

	}

}
