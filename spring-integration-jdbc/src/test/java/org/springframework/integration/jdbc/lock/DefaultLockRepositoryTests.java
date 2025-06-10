/*
 * Copyright 2021-2025 the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import java.sql.Connection;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ruslan Stelmachenko
 *
 * @since 5.3.10
 */
@SpringJUnitConfig(locations = "JdbcLockRegistryTests-context.xml")
@DirtiesContext
class DefaultLockRepositoryTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private DataSourceTransactionManager transactionManager;

	@Autowired
	private LockRepository client;

	@BeforeEach
	void clear() {
		this.client.close();
	}

	@Transactional
	@Test
	void testNewTransactionIsStartedWhenTransactionIsAlreadyActive() {
		// Make sure a transaction is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

		TransactionSynchronization transactionSynchronization = spy(TransactionSynchronization.class);
		TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);

		this.client.acquire("foo", Duration.ofSeconds(10)); // 1
		this.client.renew("foo", Duration.ofSeconds(10)); // 2
		this.client.delete("foo"); // 3
		this.client.isAcquired("foo"); // 4
		this.client.deleteExpired(); // 5
		this.client.close(); // 6

		// Make sure a transaction is still active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		// And was suspended for each invocation of @Transactional methods of DefaultLockRepository,
		// that confirms that these methods were called in a separate transaction each.
		verify(transactionSynchronization, times(6)).suspend();
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	@Test
	void testIsAcquiredFromRepeatableReadTransaction() {
		// Make sure a transaction with REPEATABLE_READ isolation level is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())
				.isEqualTo(Connection.TRANSACTION_REPEATABLE_READ);

		this.client.acquire("foo", Duration.ofSeconds(10));
		assertThat(this.client.isAcquired("foo")).isTrue();

		this.client.delete("foo");
		assertThat(this.client.isAcquired("foo")).isFalse();
	}

	@Test
	void testAcquired() {
		client.acquire("foo", Duration.ofMillis(100));
		assertThat(this.client.isAcquired("foo")).isTrue();
	}

	@Test
	void testAcquireSameLockTwiceAndTtlWillBeUpdated() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(150));
		Thread.sleep(100);
		client.acquire("foo", Duration.ofMillis(150));
		Thread.sleep(60);
		assertThat(this.client.isAcquired("foo")).isTrue();
	}

	@Test
	void testAcquiredLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("foo", Duration.ofMillis(100));
		assertThat(this.client.acquire("foo", Duration.ofMillis(100))).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testAcquiredLockIsAcquiredByAnotherProcessButExpired() throws InterruptedException {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("foo", Duration.ofMillis(100));
		Thread.sleep(110);
		assertThat(this.client.acquire("foo", Duration.ofMillis(100))).isTrue();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testIsAcquiredLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("foo", Duration.ofMillis(100));
		assertThat(this.client.isAcquired("foo")).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testIsAcquiredLockIsExpired() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(100));
		Thread.sleep(110);
		assertThat(this.client.isAcquired("foo")).isFalse();
	}

	@Test
	void testRenew() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(150));
		Thread.sleep(100);
		assertThat(client.renew("foo", Duration.ofMillis(150))).isTrue();
		Thread.sleep(60);
		assertThat(this.client.isAcquired("foo")).isTrue();
	}

	@Test
	void testRenewLockIsExpiredAndLockStatusHasBeenCleanUp() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(100));
		Thread.sleep(110);
		client.deleteExpired();

		assertThat(this.client.renew("foo", Duration.ofMillis(100))).isFalse();
	}

	@Test
	void testRenewLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("foo", Duration.ofMillis(100));
		assertThat(this.client.renew("foo", Duration.ofMillis(100))).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testDelete() {
		this.client.acquire("foo", Duration.ofSeconds(10));
		assertThat(this.client.delete("foo")).isTrue();
		assertThat(this.client.isAcquired("foo")).isFalse();
	}

	@Test
	void testDeleteLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("foo", Duration.ofSeconds(10));
		assertThat(this.client.delete("foo")).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testDeleteAfterLockIsExpiredAndLockStatusHasBeenCleanUp() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(100));
		Thread.sleep(200);
		client.deleteExpired();

		assertThat(this.client.delete("foo")).isFalse();
	}

	@Test
	void testDeleteExpired() throws InterruptedException {
		client.acquire("foo", Duration.ofMillis(100));
		Thread.sleep(200);
		client.deleteExpired();

		assertThat(this.client.renew("foo", Duration.ofMillis(100))).isFalse();
	}
}
