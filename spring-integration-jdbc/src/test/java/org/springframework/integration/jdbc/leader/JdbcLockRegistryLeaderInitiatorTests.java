/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.jdbc.leader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 4.3.1
 */
public class JdbcLockRegistryLeaderInitiatorTests {

	public static EmbeddedDatabase dataSource;

	@BeforeAll
	public static void init() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-drop-h2.sql")
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();
	}

	@AfterAll
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test
	public void testDistributedLeaderElection() throws Exception {
		CountDownLatch granted = new CountDownLatch(1);
		CountingPublisher countingPublisher = new CountingPublisher(granted);
		List<LockRegistryLeaderInitiator> initiators = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
			lockRepository.setTransactionManager(new DataSourceTransactionManager(dataSource));
			lockRepository.afterPropertiesSet();
			lockRepository.afterSingletonsInstantiated();
			LockRegistryLeaderInitiator initiator = new LockRegistryLeaderInitiator(
					new JdbcLockRegistry(lockRepository),
					new DefaultCandidate("foo", "bar"));
			initiator.setLeaderEventPublisher(countingPublisher);
			initiators.add(initiator);
		}

		for (LockRegistryLeaderInitiator initiator : initiators) {
			initiator.start();
		}

		assertThat(granted.await(10, TimeUnit.SECONDS)).isTrue();

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
		assertThat(initiator1.getContext().getRole()).isEqualTo("bar");
		assertThat(initiator2.getContext().isLeader()).isFalse();
		assertThat(initiator2.getContext().getRole()).isEqualTo("bar");

		final CountDownLatch granted1 = new CountDownLatch(1);
		final CountDownLatch granted2 = new CountDownLatch(1);
		CountDownLatch revoked1 = new CountDownLatch(1);
		CountDownLatch revoked2 = new CountDownLatch(1);
		final CountDownLatch acquireLockFailed1 = new CountDownLatch(1);
		final CountDownLatch acquireLockFailed2 = new CountDownLatch(1);

		initiator1.setLeaderEventPublisher(new CountingPublisher(granted1, revoked1, acquireLockFailed1) {

			@Override
			public void publishOnRevoked(Object source, Context context, String role) {
				try {
					// It's difficult to see round-robin election, so block one initiator until the second is elected.
					assertThat(granted2.await(20, TimeUnit.SECONDS)).isTrue();
				}
				catch (InterruptedException e) {
					// No op
				}
				super.publishOnRevoked(source, context, role);
			}

		});

		initiator2.setLeaderEventPublisher(new CountingPublisher(granted2, revoked2, acquireLockFailed2) {

			@Override
			public void publishOnRevoked(Object source, Context context, String role) {
				try {
					// It's difficult to see round-robin election, so block one initiator until the second is elected.
					assertThat(granted1.await(20, TimeUnit.SECONDS)).isTrue();
				}
				catch (InterruptedException e) {
					// No op
				}
				super.publishOnRevoked(source, context, role);
			}

		});

		initiator1.getContext().yield();

		assertThat(revoked1.await(20, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator2.getContext().isLeader()).isTrue();
		assertThat(initiator1.getContext().isLeader()).isFalse();

		initiator2.getContext().yield();

		assertThat(revoked2.await(20, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator1.getContext().isLeader()).isTrue();
		assertThat(initiator2.getContext().isLeader()).isFalse();

		// Stop second initiator, so the first one will be leader even after yield
		initiator2.stop();

		CountDownLatch granted11 = new CountDownLatch(1);
		initiator1.setLeaderEventPublisher(new CountingPublisher(granted11));

		initiator1.getContext().yield();

		assertThat(granted11.await(20, TimeUnit.SECONDS)).isTrue();
		assertThat(initiator1.getContext().isLeader()).isTrue();

		initiator1.stop();
	}

	@Test
	@Disabled("Looks like an embedded DBd is not fully cleared if we don't close application context")
	public void testLostConnection() throws InterruptedException {
		CountDownLatch granted = new CountDownLatch(1);
		CountingPublisher countingPublisher = new CountingPublisher(granted);

		DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
		lockRepository.afterPropertiesSet();
		LockRegistryLeaderInitiator initiator = new LockRegistryLeaderInitiator(new JdbcLockRegistry(lockRepository));
		initiator.setLeaderEventPublisher(countingPublisher);

		initiator.start();

		assertThat(granted.await(10, TimeUnit.SECONDS)).isTrue();

		destroy();

		assertThat(countingPublisher.revoked.await(60, TimeUnit.SECONDS)).isTrue();

		granted = new CountDownLatch(1);
		countingPublisher = new CountingPublisher(granted);
		initiator.setLeaderEventPublisher(countingPublisher);

		init();

		assertThat(granted.await(60, TimeUnit.SECONDS)).isTrue();

		initiator.stop();
	}

	private static class CountingPublisher implements LeaderEventPublisher {

		private final CountDownLatch granted;

		private final CountDownLatch revoked;

		private final CountDownLatch acquireLockFailed;

		private volatile LockRegistryLeaderInitiator initiator;

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
