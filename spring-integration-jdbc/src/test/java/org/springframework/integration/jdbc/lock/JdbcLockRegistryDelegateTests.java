/*
 * Copyright 2020-present the original author or authors.
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

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olivier Hubaut
 * @author Fran Aranda
 * @author Eddie Cho
 * @author Glenn Renfro
 *
 * @since 5.2.11
 */
class JdbcLockRegistryDelegateTests {

	private JdbcLockRegistry registry;

	private LockRepository repository;

	@BeforeEach
	public void clear() {
		repository = mock(LockRepository.class);
		registry = new JdbcLockRegistry(repository);

		when(repository.acquire(anyString(), any())).thenReturn(true);
	}

	@Test
	void testLessAmountOfUnlockThanLock() {
		final Random random = new Random();
		final int lockCount = random.nextInt(5) + 1;
		final int unlockCount = random.nextInt(lockCount);

		final Lock lock = registry.obtain("foo");
		for (int i = 0; i < lockCount; i++) {
			lock.tryLock();
		}
		for (int i = 0; i < unlockCount; i++) {
			lock.unlock();
		}

		assertThat(TestUtils.<ReentrantLock>getPropertyValue(lock, "delegate").isLocked()).isTrue();
	}

	@Test
	void testSameAmountOfUnlockThanLock() {
		final Random random = new Random();
		final int lockCount = random.nextInt(5) + 1;

		final Lock lock = registry.obtain("foo");
		when(repository.delete(anyString())).thenReturn(true);

		for (int i = 0; i < lockCount; i++) {
			lock.tryLock();
		}
		for (int i = 0; i < lockCount; i++) {
			lock.unlock();
		}

		assertThat(TestUtils.<ReentrantLock>getPropertyValue(lock, "delegate").isLocked()).isFalse();
	}

	@Test
	void testTransientDataAccessException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(mock(TransientDataAccessException.class))
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.<ReentrantLock>getPropertyValue(lock, "delegate").isLocked()).isFalse();
	}

	@Test
	void testTransactionTimedOutException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(TransactionTimedOutException.class)
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.<ReentrantLock>getPropertyValue(lock, "delegate").isLocked()).isFalse();
	}

	@Test
	void testTransactionSystemException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(TransactionSystemException.class)
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.<ReentrantLock>getPropertyValue(lock, "delegate").isLocked()).isFalse();
	}

}
