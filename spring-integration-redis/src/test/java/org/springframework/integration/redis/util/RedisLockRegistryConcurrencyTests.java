/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.util;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry.RedisLockType;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offline concurrency tests for the local Redis lock cache.
 *
 * @author Li Yifan
 */
class RedisLockRegistryConcurrencyTests {

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void cacheCheckWaitsForRegistryLock(RedisLockType lockType) throws Exception {
		RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
		RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, "cacheCheck");
		registry.setRedisLockType(lockType);
		DistributedLock lock = registry.obtain("order");
		ReentrantLock registryLock = TestUtils.getPropertyValue(registry, "lock");
		IllegalStateException connectionFailure = new IllegalStateException("Redis connection unavailable");
		when(connectionFactory.getConnection()).thenAnswer(invocation -> {
			assertThat(registryLock.isHeldByCurrentThread()).isFalse();
			throw connectionFailure;
		});
		FutureTask<Boolean> acquisition = new FutureTask<>(lock::tryLock);
		Thread worker = new Thread(acquisition, "redis-cache-check");
		try {
			registryLock.lock();
			try {
				worker.start();
				await().until(() -> registryLock.hasQueuedThread(worker) || acquisition.isDone());
				assertThat(registryLock.hasQueuedThread(worker)).isTrue();
				verify(connectionFactory, never()).getConnection();
			}
			finally {
				registryLock.unlock();
			}
			assertThatThrownBy(() -> acquisition.get(10, TimeUnit.SECONDS))
					.hasCauseInstanceOf(CannotAcquireLockException.class)
					.rootCause().isSameAs(connectionFailure);
		}
		finally {
			worker.join(10_000);
			registry.destroy();
		}
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void evictedLockIsRejectedBeforeAccessingRedis(RedisLockType lockType) {
		RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
		RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, "cacheEviction");
		registry.setRedisLockType(lockType);
		registry.setCacheCapacity(1);
		DistributedLock lock = registry.obtain("order");
		registry.obtain("shipment");
		try {
			assertThatThrownBy(lock::tryLock)
					.isInstanceOf(CannotAcquireLockException.class)
					.hasCauseInstanceOf(IllegalStateException.class)
					.rootCause().hasMessageStartingWith("The lock 'order' was evicted from the exhausted cache.");
			verify(connectionFactory, never()).getConnection();
			assertThat(TestUtils.<ReentrantLock>getPropertyValue(registry, "lock").isLocked()).isFalse();
		}
		finally {
			registry.destroy();
		}
	}

}
