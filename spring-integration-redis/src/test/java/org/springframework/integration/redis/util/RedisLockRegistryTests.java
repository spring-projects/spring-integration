/*
 * Copyright 2014-present the original author or authors.
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

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.util.RedisLockRegistry.RedisLockType;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Konstantin Yakimov
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Unseok Kim
 * @author Artem Vozhdayenko
 * @author Anton Gabov
 * @author Eddie Cho
 * @author Youbin Wu
 * @author Glenn Renfro
 *
 * @since 4.0
 *
 */
class RedisLockRegistryTests implements RedisContainerTest {

	private final Log logger = LogFactory.getLog(getClass());

	private final String registryKey = UUID.randomUUID().toString();

	private final String registryKey2 = UUID.randomUUID().toString();

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnections() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@BeforeEach
	@AfterEach
	void setupShutDown() {
		StringRedisTemplate template = this.createTemplate();
		template.delete(this.registryKey + ":*");
		template.delete(this.registryKey2 + ":*");
	}

	private StringRedisTemplate createTemplate() {
		return new StringRedisTemplate(redisConnectionFactory);
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLock(RedisLockType testRedisLockType) {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
			}
			finally {
				lock.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLockWithCustomTtl(RedisLockType testRedisLockType) throws InterruptedException {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 100);
		long sleepTimeLongerThanDefaultTTL = 200;
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 3; i++) {
			DistributedLock lock = registry.obtain("foo");
			lock.lock(Duration.ofMillis(500));
			try {
				assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
				Thread.sleep(sleepTimeLongerThanDefaultTTL);
			}
			finally {
				assertThatNoException().isThrownBy(lock::unlock);
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTryLockWithCustomTtl(RedisLockType testRedisLockType) throws InterruptedException {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 100);
		long sleepTimeLongerThanDefaultTTL = 200;
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 3; i++) {
			DistributedLock lock = registry.obtain("foo");
			lock.tryLock(Duration.ofMillis(100), Duration.ofMillis(500));
			try {
				assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
				Thread.sleep(sleepTimeLongerThanDefaultTTL);
			}
			finally {
				assertThatNoException().isThrownBy(lock::unlock);
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testUnlockAfterLockStatusHasBeenExpired(RedisLockType testRedisLockType) throws InterruptedException {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 100);
		registry.setRedisLockType(testRedisLockType);
		Lock lock = registry.obtain("foo");
		lock.lock();
		Thread.sleep(200);

		assertThatThrownBy(lock::unlock).isInstanceOf(ConcurrentModificationException.class);
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLockInterruptibly(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
			}
			finally {
				lock.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testReentrantLock(RedisLockType testRedisLockType) {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lock();
				try {
					// just get the lock
				}
				finally {
					lock2.unlock();
				}
			}
			finally {
				lock1.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testReentrantLockInterruptibly(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lockInterruptibly();
				try {
					// just get the lock
				}
				finally {
					lock2.unlock();
				}
			}
			finally {
				lock1.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoLocks(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("bar");
				assertThat(lock2).isNotSameAs(lock1);
				lock2.lockInterruptibly();
				try {
					// just get the lock
				}
				finally {
					lock2.unlock();
				}
			}
			finally {
				lock1.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoThreadsSecondFailsToGetLock(RedisLockType testRedisLockType) throws Exception {
		final RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		final Lock lock1 = registry.obtain("foo");
		lock1.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Object> result = executorService.submit(() -> {
			Lock lock2 = registry.obtain("foo");
			locked.set(lock2.tryLock(200, TimeUnit.MILLISECONDS));
			latch.countDown();
			try {
				lock2.unlock();
			}
			catch (IllegalStateException ise) {
				return ise;
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise).isInstanceOf(IllegalStateException.class);
		assertThat(((Exception) ise).getMessage()).contains("You do not own lock at");
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
		executorService.shutdown();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoThreads(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		Lock lock1 = registry.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			Lock lock2 = registry.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				assertThat(getRedisLockRegistryLocks(registry)).hasSize(1);
				latch2.await(10, TimeUnit.SECONDS);
				locked.set(true);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				lock2.unlock();
				latch3.countDown();
			}
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		latch2.countDown();
		assertThat(latch3.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isTrue();
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
		executorService.shutdown();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoThreadsDifferentRegistries(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry1 = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry1.setRedisLockType(testRedisLockType);
		RedisLockRegistry registry2 = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry2.setRedisLockType(testRedisLockType);
		Lock lock1 = registry1.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertThat(getRedisLockRegistryLocks(registry1)).hasSize(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			Lock lock2 = registry2.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				assertThat(getRedisLockRegistryLocks(registry2)).hasSize(1);
				latch2.await(10, TimeUnit.SECONDS);
				locked.set(true);
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				this.logger.error("Interrupted while locking: " + lock2, e1);
			}
			finally {
				try {
					lock2.unlock();
					latch3.countDown();
				}
				catch (IllegalStateException e2) {
					this.logger.error("Failed to unlock: " + lock2, e2);
				}
			}
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		latch2.countDown();
		assertThat(latch3.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isTrue();
		registry1.expireUnusedOlderThan(-1000);
		registry2.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry1)).isEmpty();
		assertThat(getRedisLockRegistryLocks(registry2)).isEmpty();
		registry1.destroy();
		registry2.destroy();
		executorService.shutdown();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoThreadsWrongOneUnlocks(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);
		Lock lock = registry.obtain("foo");
		lock.lockInterruptibly();
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Object> result = executorService.submit(() -> {
			try {
				lock.unlock();
			}
			catch (IllegalStateException ise) {
				latch.countDown();
				return ise;
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise).isInstanceOf(IllegalStateException.class);
		assertThat(((Exception) ise).getMessage()).contains("You do not own lock at");
		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
		executorService.shutdown();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testExpireTwoRegistries(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry1 = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 100);
		registry1.setRedisLockType(testRedisLockType);
		RedisLockRegistry registry2 = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 100);
		registry2.setRedisLockType(testRedisLockType);
		Lock lock1 = registry1.obtain("foo");
		Lock lock2 = registry2.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		assertThat(lock2.tryLock()).isFalse();
		waitForExpire("foo");
		assertThat(lock2.tryLock()).isTrue();
		assertThat(lock1.tryLock()).isFalse();
		registry1.destroy();
		registry2.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testExceptionOnExpire(RedisLockType testRedisLockType) throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 1);
		registry.setRedisLockType(testRedisLockType);
		Lock lock1 = registry.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		waitForExpire("foo");
		assertThatThrownBy(lock1::unlock)
				.isInstanceOf(ConcurrentModificationException.class)
				.hasMessageContaining("Lock was released in the store due to expiration.");
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testRenewalOnExpire(RedisLockType redisLockType) throws Exception {
		long expireAfter = 300L;
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, expireAfter);
		registry.setRenewalTaskScheduler(new SimpleAsyncTaskScheduler());
		registry.setRedisLockType(redisLockType);
		Lock lock1 = registry.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		Thread.sleep(expireAfter * 2);
		lock1.unlock();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testEquals(RedisLockType testRedisLockType) {
		RedisConnectionFactory connectionFactory = redisConnectionFactory;
		RedisLockRegistry registry1 = new RedisLockRegistry(connectionFactory, this.registryKey);
		registry1.setRedisLockType(testRedisLockType);
		RedisLockRegistry registry2 = new RedisLockRegistry(connectionFactory, this.registryKey);
		registry2.setRedisLockType(testRedisLockType);
		RedisLockRegistry registry3 = new RedisLockRegistry(connectionFactory, this.registryKey2);
		registry3.setRedisLockType(testRedisLockType);

		Lock lock1 = registry1.obtain("foo");
		Lock lock2 = registry1.obtain("foo");
		assertThat(lock2).isEqualTo(lock1);
		lock1.lock();
		lock2.lock();
		assertThat(lock2).isEqualTo(lock1);
		lock1.unlock();
		lock2.unlock();
		assertThat(lock2).isEqualTo(lock1);

		lock1 = registry1.obtain("foo");
		lock2 = registry2.obtain("foo");
		assertThat(lock2).isNotEqualTo(lock1);
		lock1.lock();
		assertThat(lock2.tryLock()).isFalse();
		lock1.unlock();

		lock1 = registry1.obtain("foo");
		lock2 = registry3.obtain("foo");
		assertThat(lock2).isNotEqualTo(lock1);
		lock1.lock();
		lock2.lock();
		lock1.unlock();
		lock2.unlock();
		registry1.destroy();
		registry2.destroy();
		registry3.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testThreadLocalListLeaks(RedisLockType testRedisLockType) {
		RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey, 10000);
		registry.setRedisLockType(testRedisLockType);

		for (int i = 0; i < 10; i++) {
			registry.obtain("foo" + i);
		}
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(10);

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			lock.lock();
		}
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(10);

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			lock.unlock();
		}
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(10);
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testExpireNotChanged(RedisLockType testRedisLockType) throws Exception {
		RedisConnectionFactory connectionFactory = redisConnectionFactory;
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setRedisLockType(testRedisLockType);

		Lock lock = registry.obtain("foo");
		lock.lock();

		Long expire = getExpire(registry, "foo");

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Object> result = executorService.submit(() -> {
			Lock lock2 = registry.obtain("foo");
			assertThat(lock2.tryLock()).isFalse();
			return null;
		});
		result.get();
		assertThat(getExpire(registry, "foo")).isEqualTo(expire);
		lock.unlock();
		registry.destroy();
		executorService.shutdown();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void concurrentObtainCapacityTest(RedisLockType testRedisLockType) throws InterruptedException {
		final int KEY_CNT = 500;
		final int CAPACITY_CNT = 179;
		final int THREAD_CNT = 4;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = redisConnectionFactory;
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.setRedisLockType(testRedisLockType);

		final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_CNT);

		for (int i = 0; i < KEY_CNT; i++) {
			int finalI = i;
			executorService.submit(() -> {
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				String keyId = "foo:" + finalI;
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		//capacity limit test
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(CAPACITY_CNT);

		registry.expireUnusedOlderThan(-1000);
		assertThat(getRedisLockRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void concurrentObtainRemoveOrderTest(RedisLockType testRedisLockType) throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = redisConnectionFactory;
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.setRedisLockType(testRedisLockType);

		final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_CNT);
		final Queue<String> remainLockCheckQueue = new LinkedBlockingQueue<>();

		//Removed due to capcity limit
		for (int i = 0; i < DUMMY_LOCK_CNT; i++) {
			Lock obtainLock0 = registry.obtain("foo:" + i);
			obtainLock0.lock();
			obtainLock0.unlock();
		}

		for (int i = DUMMY_LOCK_CNT; i < THREAD_CNT + DUMMY_LOCK_CNT; i++) {
			int finalI = i;
			executorService.submit(() -> {
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				String keyId = "foo:" + finalI;
				remainLockCheckQueue.offer(keyId);
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRedisLockRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[0]));
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void concurrentObtainAccessRemoveOrderTest(RedisLockType testRedisLockType) throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT + 1;
		final String REMAIN_DUMMY_LOCK_KEY = "foo:1";

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = redisConnectionFactory;
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.setRedisLockType(testRedisLockType);

		final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_CNT);
		final Queue<String> remainLockCheckQueue = new LinkedBlockingQueue<>();

		//Removed due to capcity limit
		for (int i = 0; i < DUMMY_LOCK_CNT; i++) {
			Lock obtainLock0 = registry.obtain("foo:" + i);
			obtainLock0.lock();
			obtainLock0.unlock();
		}

		Lock obtainLock0 = registry.obtain(REMAIN_DUMMY_LOCK_KEY);
		obtainLock0.lock();
		obtainLock0.unlock();
		remainLockCheckQueue.offer(REMAIN_DUMMY_LOCK_KEY);

		for (int i = DUMMY_LOCK_CNT; i < THREAD_CNT + DUMMY_LOCK_CNT; i++) {
			int finalI = i;
			executorService.submit(() -> {
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				String keyId = "foo:" + finalI;
				remainLockCheckQueue.offer(keyId);
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRedisLockRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[0]));
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void setCapacityTest(RedisLockType testRedisLockType) {
		final int CAPACITY_CNT = 4;
		final RedisConnectionFactory connectionFactory = redisConnectionFactory;
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.setRedisLockType(testRedisLockType);

		registry.obtain("foo:1");
		registry.obtain("foo:2");
		registry.obtain("foo:3");

		//capacity 4->3
		registry.setCacheCapacity(CAPACITY_CNT - 1);

		registry.obtain("foo:4");

		assertThat(getRedisLockRegistryLocks(registry)).hasSize(3);
		assertThat(getRedisLockRegistryLocks(registry)).containsKeys("foo:2", "foo:3", "foo:4");

		//capacity 3->4
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.obtain("foo:5");
		assertThat(getRedisLockRegistryLocks(registry)).hasSize(4);
		assertThat(getRedisLockRegistryLocks(registry)).containsKeys("foo:3", "foo:4", "foo:5");
		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void twoRedisLockRegistryTest(RedisLockType testRedisLockType) throws InterruptedException {
		RedisLockRegistry registry1 = new RedisLockRegistry(redisConnectionFactory, registryKey, 1000000L);
		registry1.setRedisLockType(testRedisLockType);
		RedisLockRegistry registry2 = new RedisLockRegistry(redisConnectionFactory, registryKey, 1000000L);
		registry2.setRedisLockType(testRedisLockType);

		String lockKey = "test-1";

		Lock obtainLock_1 = registry1.obtain(lockKey);
		Lock obtainLock_2 = registry2.obtain(lockKey);

		CountDownLatch registry1Lock = new CountDownLatch(1);
		CountDownLatch endDownLatch = new CountDownLatch(2);

		CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
			try {
				obtainLock_1.lock();
				//				for (int i = 0; i < 10; i++) {
				//					Thread.sleep(1000);
				//				}
				registry1Lock.countDown();
				obtainLock_1.unlock();
				endDownLatch.countDown();
			}
			catch (Exception ignore) {
				ignore.printStackTrace();
			}
		});

		CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
			try {
				registry1Lock.await();
			}
			catch (InterruptedException ignore) {
			}
			obtainLock_2.lock();
			obtainLock_2.unlock();
			endDownLatch.countDown();
		});

		endDownLatch.await();

		assertThat(future1).isNotCompletedExceptionally();
		assertThat(future2).isNotCompletedExceptionally();
		registry1.destroy();
		registry2.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void multiRedisLockRegistryTest(RedisLockType testRedisLockType) throws InterruptedException, ExecutionException {
		final String testKey = "testKey";
		final long expireAfter = 100000L;
		final int lockRegistryNum = 10;
		final ExecutorService executorService = Executors.newFixedThreadPool(lockRegistryNum * 2);
		final AtomicInteger atomicInteger = new AtomicInteger(0);
		final List<Callable<Boolean>> collect = IntStream.range(0, lockRegistryNum)
				.mapToObj((num) -> new RedisLockRegistry(
						redisConnectionFactory, registryKey, expireAfter))
				.map((registry) -> {
					registry.setRedisLockType(testRedisLockType);
					final Callable<Boolean> callable = () -> {
						Lock obtain = registry.obtain(testKey);
						obtain.lock();
						obtain.unlock();
						atomicInteger.incrementAndGet();
						return true;
					};
					return callable;
				})
				.collect(Collectors.toList());

		final int testCnt = 3;
		for (int i = 0; i < testCnt; i++) {
			List<Future<Boolean>> futures_1 = executorService.invokeAll(collect);
			for (Future<Boolean> fu : futures_1) {
				fu.get();
			}
		}

		assertThat(atomicInteger.get()).isEqualTo(testCnt * lockRegistryNum);
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void earlyWakeUpTest(RedisLockType testRedisLockType) throws InterruptedException {
		final int THREAD_CNT = 2;
		final String testKey = "testKey";

		final CountDownLatch tryLockReady = new CountDownLatch(THREAD_CNT);
		final CountDownLatch awaitTimeout = new CountDownLatch(THREAD_CNT);
		final RedisLockRegistry registry1 = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry1.setRedisLockType(testRedisLockType);
		final RedisLockRegistry registry2 = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry2.setRedisLockType(testRedisLockType);
		final RedisLockRegistry registry3 = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry3.setRedisLockType(testRedisLockType);

		final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_CNT);

		Lock lock1 = registry1.obtain(testKey);
		Lock lock2 = registry2.obtain(testKey);
		Lock lock3 = registry3.obtain(testKey);
		AtomicInteger expectOne = new AtomicInteger();

		lock1.lock();
		executorService.submit(() -> {
			try {
				tryLockReady.countDown();
				boolean b = lock2.tryLock(10, TimeUnit.SECONDS);
				awaitTimeout.countDown();
				if (b) {
					expectOne.incrementAndGet();
				}
			}
			catch (InterruptedException ignore) {
			}
		});

		executorService.submit(() -> {
			try {
				tryLockReady.countDown();
				boolean b = lock3.tryLock(10, TimeUnit.SECONDS);
				awaitTimeout.countDown();
				if (b) {
					expectOne.incrementAndGet();
				}
			}
			catch (InterruptedException ignore) {
			}
		});

		assertThat(tryLockReady.await(10, TimeUnit.SECONDS)).isTrue();
		lock1.unlock();
		assertThat(awaitTimeout.await(1, TimeUnit.SECONDS)).isFalse();
		assertThat(expectOne.get()).isEqualTo(1);
		executorService.shutdown();
		registry1.destroy();
		registry2.destroy();
		registry3.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testTwoThreadsRemoveAndObtainSameLockSimultaneously(RedisLockType testRedisLockType) throws Exception {
		final int TEST_CNT = 200;
		final long EXPIRATION_TIME_MILLIS = 10000;
		final long LOCK_WAIT_TIME_MILLIS = 500;
		final String testKey = "testKey";

		final RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(testRedisLockType);

		for (int i = 0; i < TEST_CNT; i++) {
			final String lockKey = testKey + i;
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<Lock> lock1 = new AtomicReference<>();
			final AtomicReference<Lock> lock2 = new AtomicReference<>();

			Thread thread1 = new Thread(() -> {
				try {
					latch.await();
					// remove lock
					registry.expireUnusedOlderThan(EXPIRATION_TIME_MILLIS);
					// obtain new lock and try to acquire
					Lock lock = registry.obtain(lockKey);
					lock.tryLock(LOCK_WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
					lock.unlock();

					lock1.set(lock);
				}
				catch (InterruptedException ignore) {
				}
			});

			Thread thread2 = new Thread(() -> {
				try {
					latch.await();
					// remove lock
					registry.expireUnusedOlderThan(EXPIRATION_TIME_MILLIS);
					// obtain new lock and try to acquire
					Lock lock = registry.obtain(lockKey);
					lock.tryLock(LOCK_WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
					lock.unlock();

					lock2.set(lock);
				}
				catch (InterruptedException ignore) {
				}
			});

			thread1.start();
			thread2.start();
			latch.countDown();
			thread1.join();
			thread2.join();

			// locks must be the same!
			assertThat(lock1.get()).isEqualTo(lock2.get());
		}

		registry.destroy();
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLockRenew(RedisLockType redisLockType) {
		final RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(redisLockType);
		final Lock lock = registry.obtain("foo");

		assertThat(lock.tryLock()).isTrue();
		try {
			registry.renewLock("foo");
		}
		finally {
			lock.unlock();
		}
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLockRenewLockNotOwned(RedisLockType redisLockType) {
		final RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(redisLockType);
		registry.obtain("foo");

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> registry.renewLock("foo"));
	}

	@ParameterizedTest
	@EnumSource(RedisLockType.class)
	void testLockRenewWithCustomTtl(RedisLockType redisLockType) throws InterruptedException {
		final RedisLockRegistry registry = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		final RedisLockRegistry registryOfAnotherProcess = new RedisLockRegistry(redisConnectionFactory, this.registryKey);
		registry.setRedisLockType(redisLockType);
		registryOfAnotherProcess.setRedisLockType(redisLockType);
		final DistributedLock lock = registry.obtain("foo");
		final Lock lockOfAnotherProcess = registryOfAnotherProcess.obtain("foo");
		long ttl = 100;
		long sleepTimeLongerThanTtl = 110;
		assertThat(lock.tryLock(Duration.ofMillis(100), Duration.ofMillis(ttl))).isTrue();
		try {
			registry.renewLock("foo", Duration.ofSeconds(2));
			Thread.sleep(sleepTimeLongerThanTtl);
			assertThat(lockOfAnotherProcess.tryLock(100, TimeUnit.MILLISECONDS)).isFalse();
		}
		finally {
			lock.unlock();
		}
		registryOfAnotherProcess.destroy();
	}

	@Test
	void testInitialiseWithCustomExecutor() {
		RedisLockRegistry redisLockRegistry = new RedisLockRegistry(redisConnectionFactory, "registryKey");
		redisLockRegistry.setRedisLockType(RedisLockType.PUB_SUB_LOCK);
		assertThatNoException().isThrownBy(() -> redisLockRegistry.setExecutor(mock()));
	}

	private Long getExpire(RedisLockRegistry registry, String lockKey) {
		StringRedisTemplate template = createTemplate();
		String registryKey = TestUtils.getPropertyValue(registry, "registryKey");
		return template.getExpire(registryKey + ":" + lockKey);
	}

	private void waitForExpire(String key) throws Exception {
		StringRedisTemplate template = createTemplate();
		int n = 0;
		while (n++ < 100 && template.keys(this.registryKey + ":" + key).size() > 0) {
			Thread.sleep(100);
		}
		assertThat(n < 100).as(key + " key did not expire").isTrue();
	}

	private static Map<String, Lock> getRedisLockRegistryLocks(RedisLockRegistry registry) {
		return TestUtils.getPropertyValue(registry, "locks");
	}

}
