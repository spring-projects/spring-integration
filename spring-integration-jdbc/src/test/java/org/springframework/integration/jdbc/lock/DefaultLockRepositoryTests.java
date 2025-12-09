/*
 * Copyright 2021-present the original author or authors.
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
import org.junitpioneer.jupiter.RetryingTest;

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
 * @author Artem Bilan
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

	@Test
	void testAcquired() {
		this.client.acquire("test", Duration.ofSeconds(10));
		assertThat(this.client.isAcquired("test")).isTrue();

		this.client.delete("test");
	}

	@Test
	void testDelete() {
		this.client.acquire("test", Duration.ofSeconds(10));
		assertThat(this.client.delete("test")).isTrue();
		assertThat(this.client.isAcquired("test")).isFalse();
	}

	@Transactional
	@Test
	void testNewTransactionIsStartedWhenTransactionIsAlreadyActive() {
		// Make sure a transaction is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

		TransactionSynchronization transactionSynchronization = spy(TransactionSynchronization.class);
		TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);

		this.client.acquire("test", Duration.ofSeconds(10)); // 1
		this.client.renew("test", Duration.ofSeconds(10)); // 2
		this.client.delete("test"); // 3
		this.client.isAcquired("test"); // 4
		this.client.deleteExpired(); // 5
		this.client.close(); // 6

		// Make sure a transaction is still active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		// And was suspended for each invocation of @Transactional methods of DefaultLockRepository,
		// which confirms that these methods were called in a separate transaction each.
		verify(transactionSynchronization, times(6)).suspend();
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ)
	@Test
	void testIsAcquiredFromRepeatableReadTransaction() {
		// Make sure a transaction with REPEATABLE_READ isolation level is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())
				.isEqualTo(Connection.TRANSACTION_REPEATABLE_READ);

		this.client.acquire("test", Duration.ofSeconds(10));
		assertThat(this.client.isAcquired("test")).isTrue();

		this.client.delete("test");
		assertThat(this.client.isAcquired("test")).isFalse();
	}

	@RetryingTest(10)
	void testAcquireSameLockTwiceAndTtlWillBeUpdated() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(150));
		Thread.sleep(10);
		client.acquire("test", Duration.ofSeconds(10));
		Thread.sleep(60);
		assertThat(this.client.isAcquired("test")).isTrue();
		this.client.delete("test");
	}

	@Test
	void testAcquiredLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("test", Duration.ofMillis(100));
		assertThat(this.client.acquire("test", Duration.ofMillis(100))).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@RetryingTest(10)
	void testAcquiredLockIsAcquiredByAnotherProcessButExpired() throws InterruptedException {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("test", Duration.ofMillis(100));
		Thread.sleep(110);
		assertThat(this.client.acquire("test", Duration.ofMillis(100))).isTrue();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testIsAcquiredLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("test", Duration.ofMillis(100));
		assertThat(this.client.isAcquired("test")).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@RetryingTest(10)
	void testIsAcquiredLockIsExpired() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(100));
		Thread.sleep(110);
		assertThat(this.client.isAcquired("test")).isFalse();
	}

	@RetryingTest(10)
	void testRenew() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(150));
		Thread.sleep(10);
		assertThat(client.renew("test", Duration.ofSeconds(1))).isTrue();
		Thread.sleep(60);
		assertThat(this.client.isAcquired("test")).isTrue();
		this.client.delete("test");
	}

	@RetryingTest(10)
	void testRenewLockIsExpiredAndLockStatusHasBeenCleanUp() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(100));
		Thread.sleep(110);
		client.deleteExpired();

		assertThat(this.client.renew("test", Duration.ofMillis(100))).isFalse();
	}

	@Test
	void testRenewLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("test", Duration.ofMillis(100));
		assertThat(this.client.renew("test", Duration.ofMillis(100))).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@Test
	void testDeleteLockIsAcquiredByAnotherProcess() {
		DefaultLockRepository lockRepositoryOfAnotherProcess = new DefaultLockRepository(dataSource);
		lockRepositoryOfAnotherProcess.setTransactionManager(transactionManager);
		lockRepositoryOfAnotherProcess.afterSingletonsInstantiated();
		lockRepositoryOfAnotherProcess.afterPropertiesSet();

		lockRepositoryOfAnotherProcess.acquire("test", Duration.ofSeconds(10));
		assertThat(this.client.delete("test")).isFalse();

		lockRepositoryOfAnotherProcess.close();
	}

	@RetryingTest(10)
	void testDeleteAfterLockIsExpiredAndLockStatusHasBeenCleanUp() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(10));
		Thread.sleep(50);
		client.deleteExpired();

		assertThat(this.client.delete("test")).isFalse();
	}

	@RetryingTest(10)
	void testDeleteExpired() throws InterruptedException {
		client.acquire("test", Duration.ofMillis(10));
		Thread.sleep(50);
		client.deleteExpired();

		assertThat(this.client.renew("test", Duration.ofMillis(100))).isFalse();
	}

}
