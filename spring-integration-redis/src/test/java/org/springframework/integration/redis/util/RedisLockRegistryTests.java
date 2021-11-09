/*
 * Copyright 2014-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @author Konstantin Yakimov
 * @author Artem Bilan
 * @author Vedran Pavic
 * @aythor Unseok Kim
 *
 * @since 4.0
 *
 */
public class RedisLockRegistryTests extends RedisAvailableTests {

	private final Log logger = LogFactory.getLog(getClass());

	private final String registryKey = UUID.randomUUID().toString();

	private final String registryKey2 = UUID.randomUUID().toString();

	@Before
	@After
	public void setupShutDown() {
		StringRedisTemplate template = this.createTemplate();
		template.delete(this.registryKey + ":*");
		template.delete(this.registryKey2 + ":*");
	}

	private StringRedisTemplate createTemplate() {
		return new StringRedisTemplate(getConnectionFactoryForTest());
	}

	@Test
	@RedisAvailable
	public void testLock() {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testLockInterruptibly() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}
		registry.expireUnusedOlderThan(-1000);
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testReentrantLock() {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testReentrantLockInterruptibly() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testTwoLocks() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsSecondFailsToGetLock() throws Exception {
		final RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		final Lock lock1 = registry.obtain("foo");
		lock1.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = Executors.newSingleThreadExecutor().submit(() -> {
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testTwoThreads() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		Lock lock1 = registry.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			Lock lock2 = registry.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsDifferentRegistries() throws Exception {
		RedisLockRegistry registry1 = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		RedisLockRegistry registry2 = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		Lock lock1 = registry1.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertThat(TestUtils.getPropertyValue(registry1, "locks", Map.class).size()).isEqualTo(1);
		Executors.newSingleThreadExecutor().execute(() -> {
			Lock lock2 = registry2.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				assertThat(TestUtils.getPropertyValue(registry2, "locks", Map.class).size()).isEqualTo(1);
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
		assertThat(TestUtils.getPropertyValue(registry1, "locks", Map.class).size()).isEqualTo(0);
		assertThat(TestUtils.getPropertyValue(registry2, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey);
		Lock lock = registry.obtain("foo");
		lock.lockInterruptibly();
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = Executors.newSingleThreadExecutor().submit(() -> {
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void testExpireTwoRegistries() throws Exception {
		RedisLockRegistry registry1 = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey, 100);
		RedisLockRegistry registry2 = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey, 100);
		Lock lock1 = registry1.obtain("foo");
		Lock lock2 = registry2.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		assertThat(lock2.tryLock()).isFalse();
		waitForExpire("foo");
		assertThat(lock2.tryLock()).isTrue();
		assertThat(lock1.tryLock()).isFalse();
	}

	@Test
	@RedisAvailable
	public void testExceptionOnExpire() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey, 1);
		Lock lock1 = registry.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		waitForExpire("foo");
		assertThatIllegalStateException()
				.isThrownBy(lock1::unlock)
				.withMessageContaining("Lock was released in the store due to expiration.");
	}


	@Test
	@RedisAvailable
	public void testEquals() {
		RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		RedisLockRegistry registry1 = new RedisLockRegistry(connectionFactory, this.registryKey);
		RedisLockRegistry registry2 = new RedisLockRegistry(connectionFactory, this.registryKey);
		RedisLockRegistry registry3 = new RedisLockRegistry(connectionFactory, this.registryKey2);
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
	}

	@Test
	@RedisAvailable
	public void testThreadLocalListLeaks() {
		RedisLockRegistry registry = new RedisLockRegistry(getConnectionFactoryForTest(), this.registryKey, 10000);

		for (int i = 0; i < 10; i++) {
			registry.obtain("foo" + i);
		}
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(10);

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			lock.lock();
		}
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(10);

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			lock.unlock();
		}
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(10);
	}

	@Test
	@RedisAvailable
	public void testExpireNotChanged() throws Exception {
		RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		Lock lock = registry.obtain("foo");
		lock.lock();

		Long expire = getExpire(registry, "foo");

		Future<Object> result = Executors.newSingleThreadExecutor().submit(() -> {
			Lock lock2 = registry.obtain("foo");
			assertThat(lock2.tryLock()).isFalse();
			return null;
		});
		result.get();
		assertThat(getExpire(registry, "foo")).isEqualTo(expire);
		lock.unlock();
	}

	@Test
	@RedisAvailable
	public void concurrentObtainCapacityTest() throws InterruptedException {
		final int KEY_CNT = 500;
		final int CAPACITY_CNT = 179;
		final int THREAD_CNT = 4;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCapacity(CAPACITY_CNT);
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
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(CAPACITY_CNT);


		registry.expireUnusedOlderThan(-1000);
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	@RedisAvailable
	public void concurrentObtainRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCapacity(CAPACITY_CNT);
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
				remainLockCheckQueue.toArray(new String[remainLockCheckQueue.size()]));
	}

	@Test
	@RedisAvailable
	public void concurrentObtainAccessRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT + 1;
		final String REMAIN_DUMMY_LOCK_KEY = "foo:1";

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCapacity(CAPACITY_CNT);
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
				remainLockCheckQueue.toArray(new String[remainLockCheckQueue.size()]));
	}

	@Test
	@RedisAvailable
	public void setCapacityTest() {
		final int CAPACITY_CNT = 4;
		final RedisConnectionFactory connectionFactory = getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		registry.setCapacity(CAPACITY_CNT);

		Lock obtain1 = registry.obtain("foo:1");
		Lock obtain2 = registry.obtain("foo:2");
		Lock obtain3 = registry.obtain("foo:3");

		//capacity 4->3
		registry.setCapacity(CAPACITY_CNT - 1);

		Lock obtain4 = registry.obtain("foo:4");

		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(3);
		assertThat(getRedisLockRegistryLocks(registry)).containsKeys(
				new String[] { "foo:2", "foo:3", "foo:4" });

		//capacity 3->4
		registry.setCapacity(CAPACITY_CNT);
		Lock obtain5 = registry.obtain("foo:5");
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(4);
		assertThat(getRedisLockRegistryLocks(registry)).containsKeys(
				new String[] { "foo:3", "foo:4", "foo:5" });
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void ntestUlink() {
		RedisOperations ops = mock(RedisOperations.class);
		Properties props = new Properties();
		willReturn(props).given(ops).execute(any(RedisCallback.class));
		props.setProperty("redis_version", "3.0.0");
		RedisLockRegistry registry = new RedisLockRegistry(mock(RedisConnectionFactory.class), "foo");
		assertThat(TestUtils.getPropertyValue(registry, "ulinkAvailable", Boolean.class)).isFalse();
		props.setProperty("redis_version", "4.0.0");
		registry = new RedisLockRegistry(mock(RedisConnectionFactory.class), "foo");
		assertThat(TestUtils.getPropertyValue(registry, "ulinkAvailable", Boolean.class)).isTrue();
	}

	private Long getExpire(RedisLockRegistry registry, String lockKey) {
		StringRedisTemplate template = createTemplate();
		String registryKey = TestUtils.getPropertyValue(registry, "registryKey", String.class);
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

	@SuppressWarnings("unchecked")
	private Map<String, Lock> getRedisLockRegistryLocks(RedisLockRegistry registry) {
		return TestUtils.getPropertyValue(registry, "locks", Map.class);
	}
}
