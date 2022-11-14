/*
 * Copyright 2021-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
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
public class DefaultLockRepositoryTests {

	@Autowired
	private LockRepository client;

	@BeforeEach
	public void clear() {
		this.client.close();
	}

	@Transactional
	@Test
	public void testNewTransactionIsStartedWhenTransactionIsAlreadyActive() {
		// Make sure a transaction is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

		TransactionSynchronization transactionSynchronization = spy(TransactionSynchronization.class);
		TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);

		this.client.acquire("foo"); // 1
		this.client.renew("foo"); // 2
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
	public void testIsAcquiredFromRepeatableReadTransaction() {
		// Make sure a transaction with REPEATABLE_READ isolation level is active
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())
				.isEqualTo(Connection.TRANSACTION_REPEATABLE_READ);

		this.client.acquire("foo");
		assertThat(this.client.isAcquired("foo")).isTrue();

		this.client.delete("foo");
		assertThat(this.client.isAcquired("foo")).isFalse();
	}

}
