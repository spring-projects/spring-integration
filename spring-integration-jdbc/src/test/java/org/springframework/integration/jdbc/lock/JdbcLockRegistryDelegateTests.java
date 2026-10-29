/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.jdbc.lock;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olivier Hubaut
 * @author Fran Aranda
 * @author Eddie Cho
 * @author Jiwoo Lee
 * @author Li Yifan
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
	void cacheCheckWaitsForRegistryLock() throws Exception {
		DistributedLock lock = this.registry.obtain("order");
		ReentrantLock registryLock = TestUtils.getPropertyValue(this.registry, "lock", ReentrantLock.class);
		when(this.repository.acquire(anyString(), any())).thenAnswer(invocation -> {
			assertThat(registryLock.isHeldByCurrentThread()).isFalse();
			return false;
		});
		FutureTask<Boolean> acquisition = new FutureTask<>(lock::tryLock);
		Thread worker = new Thread(acquisition, "jdbc-cache-check");
		registryLock.lock();
		try {
			worker.start();
			await().until(() -> registryLock.hasQueuedThread(worker) || acquisition.isDone());
			assertThat(registryLock.hasQueuedThread(worker)).isTrue();
			verify(this.repository, never()).acquire(anyString(), any());
		}
		finally {
			registryLock.unlock();
			await().until(acquisition::isDone);
		}
		assertThat(acquisition.get()).isFalse();
		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	void evictedLockIsRejectedBeforeAccessingRepository() {
		this.registry.setCacheCapacity(1);
		DistributedLock lock = this.registry.obtain("order");
		this.registry.obtain("shipment");
		assertThatThrownBy(lock::tryLock)
				.isInstanceOf(CannotAcquireLockException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.rootCause().hasMessageStartingWith("The lock 'order' was evicted from the exhausted cache");
		verify(this.repository, never()).acquire(anyString(), any());
		assertThat(TestUtils.getPropertyValue(this.registry, "lock", ReentrantLock.class).isLocked()).isFalse();
		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	void testLockRestoresInterruptedStatus() {
		final AtomicInteger attempts = new AtomicInteger();
		when(repository.acquire(anyString(), any())).thenAnswer(invocation -> attempts.incrementAndGet() > 1);
		when(repository.delete(anyString())).thenReturn(true);
		registry.setIdleBetweenTries(Duration.ofMillis(10));
		final DistributedLock lock = registry.obtain("foo");

		Thread.currentThread().interrupt();
		try {
			lock.lock(Duration.ofSeconds(10));

			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		}
		finally {
			Thread.interrupted();
			lock.unlock();
		}
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

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isTrue();
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

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	void testTransientDataAccessException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(mock(TransientDataAccessException.class))
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	void testTransactionTimedOutException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(TransactionTimedOutException.class)
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

	@Test
	void testTransactionSystemException() {
		final Lock lock = registry.obtain("foo");
		lock.tryLock();

		when(repository.delete(anyString()))
				.thenThrow(TransactionSystemException.class)
				.thenReturn(true);

		lock.unlock();

		assertThat(TestUtils.getPropertyValue(lock, "delegate", ReentrantLock.class).isLocked()).isFalse();
	}

}
