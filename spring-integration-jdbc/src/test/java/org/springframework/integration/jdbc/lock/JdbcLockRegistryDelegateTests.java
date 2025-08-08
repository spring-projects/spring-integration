/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.jdbc.lock;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olivier Hubaut
 * @author Fran Aranda
 *
 * @since 5.2.11
 */
public class JdbcLockRegistryDelegateTests {

	private JdbcLockRegistry registry;

	private LockRepository repository;

	@BeforeEach
	public void clear() {
		repository = mock(LockRepository.class);
		registry = new JdbcLockRegistry(repository);

		when(repository.acquire(anyString())).thenReturn(true);
	}

	@Test
	public void testLessAmountOfUnlockThanLock() {
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

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isTrue();
	}

	@Test
	public void testSameAmountOfUnlockThanLock() {
		final Random random = new Random();
		final int lockCount = random.nextInt(5) + 1;

		final Lock lock = registry.obtain("foo");
		for (int i = 0; i < lockCount; i++) {
			lock.tryLock();
		}
		for (int i = 0; i < lockCount; i++) {
			lock.unlock();
		}

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	public void testTransientDataAccessException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		final AtomicBoolean shouldThrow = new AtomicBoolean(true);
		doAnswer(invocation -> {
			if (shouldThrow.getAndSet(false)) {
				throw mock(TransientDataAccessException.class);
			}
			return null;
		}).when(repository).delete(anyString());

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	public void testTransactionTimedOutException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		final AtomicBoolean shouldThrow = new AtomicBoolean(true);
		doAnswer(invocation -> {
			if (shouldThrow.getAndSet(false)) {
				throw mock(TransactionTimedOutException.class);
			}
			return null;
		}).when(repository).delete(anyString());

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	public void testTransactionSystemException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		final AtomicBoolean shouldThrow = new AtomicBoolean(true);
		doAnswer(invocation -> {
			if (shouldThrow.getAndSet(false)) {
				throw mock(TransactionSystemException.class);
			}
			return null;
		}).when(repository).delete(anyString());

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

}
