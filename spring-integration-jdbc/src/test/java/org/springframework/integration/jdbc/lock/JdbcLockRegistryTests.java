/*
 * Copyright 2016-present the original author or authors.
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

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Stefan Vassilev
 * @author Alexandre Strubel
 * @author Unseok Kim
 * @author Eddie Cho
 * @author Glenn Renfro
 *
 * @since 4.3
 */
@SpringJUnitConfig
@DirtiesContext
class JdbcLockRegistryTests {

	private final AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Autowired
	private JdbcLockRegistry registry;

	@Autowired
	private LockRepository client;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ApplicationContext context;

	@BeforeEach
	public void clear() {
		this.registry.expireUnusedOlderThan(0);
		this.client.close();
	}

	@Test
	void testLock() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.registry.obtain("foo");
			lock.lock();
			try {
				assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(this.registry, "locks").size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		this.registry.expireUnusedOlderThan(0);
		assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(this.registry, "locks")).isEmpty();
	}

	@Test
	void testLockInterruptibly() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(this.registry, "locks").size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLockWithCustomTtl() throws Exception {
		JdbcLockRegistry lockRegistry = new JdbcLockRegistry(client, Duration.ofMillis(100));
		long sleepTimeLongerThanDefaultTTL = 110;
		for (int i = 0; i < 10; i++) {
			DistributedLock lock = lockRegistry.obtain("foo");
			lock.lock(Duration.ofMillis(200));
			try {
				assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(lockRegistry, "locks")).hasSize(1);
				Thread.sleep(sleepTimeLongerThanDefaultTTL);
			}
			finally {
				lock.unlock();
			}
		}

		lockRegistry.expireUnusedOlderThan(0);
		assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(lockRegistry, "locks")).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testTryLockWithCustomTtl() throws Exception {
		JdbcLockRegistry lockRegistry = new JdbcLockRegistry(client, Duration.ofMillis(100));
		long sleepTimeLongerThanDefaultTTL = 110;
		for (int i = 0; i < 10; i++) {
			DistributedLock lock = lockRegistry.obtain("foo");
			lock.tryLock(Duration.ofMillis(100), Duration.ofMillis(200));
			try {
				assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(lockRegistry, "locks")).hasSize(1);
				Thread.sleep(sleepTimeLongerThanDefaultTTL);
			}
			finally {
				lock.unlock();
			}
		}

		lockRegistry.expireUnusedOlderThan(0);
		assertThat(TestUtils.<Map<String, Lock>>getPropertyValue(lockRegistry, "locks")).isEmpty();
	}

	@Test
	void testReentrantLock() {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.registry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = this.registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lock();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void testReentrantLockInterruptibly() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = this.registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void testReentrantLockAfterExpiration() throws Exception {
		DefaultLockRepository client = new DefaultLockRepository(dataSource);
		client.setApplicationContext(this.context);
		client.afterPropertiesSet();
		client.afterSingletonsInstantiated();
		JdbcLockRegistry registry = new JdbcLockRegistry(client, Duration.ofMillis(1));
		Lock lock1 = registry.obtain("foo");
		assertThat(lock1.tryLock()).isTrue();
		Thread.sleep(100);
		try {
			Lock lock2 = registry.obtain("foo");
			assertThat(lock2).isSameAs(lock1);
			assertThat(lock2.tryLock()).isTrue();
			lock2.unlock();
		}
		finally {
			lock1.unlock();
		}
	}

	@Test
	void testTwoLocks() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = this.registry.obtain("bar");
				assertThat(lock2).isNotSameAs(lock1);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void testTwoThreadsSecondFailsToGetLock() throws Exception {
		final Lock lock1 = this.registry.obtain("foo");
		lock1.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = this.taskExecutor.submit(() -> {
			Lock lock2 = JdbcLockRegistryTests.this.registry.obtain("foo");
			locked.set(lock2.tryLock(200, TimeUnit.MILLISECONDS));
			latch.countDown();
			try {
				lock2.unlock();
			}
			catch (Exception e) {
				return e;
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) ise).getMessage()).contains("own");
	}

	@Test
	void testTwoThreads() throws Exception {
		final Lock lock1 = this.registry.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		this.taskExecutor.execute(() -> {
			Lock lock2 = JdbcLockRegistryTests.this.registry.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
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
	}

	@Test
	void testTwoThreadsDifferentRegistries() throws Exception {
		for (int i = 0; i < 100; i++) {

			final JdbcLockRegistry registry1 = new JdbcLockRegistry(this.client);
			DefaultLockRepository client2 = new DefaultLockRepository(this.dataSource);
			client2.setTransactionManager(this.transactionManager);
			client2.afterPropertiesSet();
			client2.afterSingletonsInstantiated();
			final JdbcLockRegistry registry2 = new JdbcLockRegistry(client2);
			final Lock lock1 = registry1.obtain("foo");
			final AtomicBoolean locked = new AtomicBoolean();
			final CountDownLatch latch1 = new CountDownLatch(1);
			final CountDownLatch latch2 = new CountDownLatch(1);
			final CountDownLatch latch3 = new CountDownLatch(1);
			lock1.lockInterruptibly();
			this.taskExecutor.execute(() -> {
				Lock lock2 = registry2.obtain("foo");
				try {
					latch1.countDown();
					lock2.lockInterruptibly();
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

		}
	}

	@Test
	void testTwoThreadsWrongOneUnlocks() throws Exception {
		final Lock lock = this.registry.obtain("foo");
		lock.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result =
				this.taskExecutor.submit(() -> {
					try {
						lock.unlock();
					}
					catch (Exception e) {
						latch.countDown();
						return e;
					}
					return null;
				});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock.unlock();
		Object imse = result.get(10, TimeUnit.SECONDS);
		assertThat(imse).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) imse).getMessage()).contains("own");
	}

	@Test
	void testLockRenew() {
		final Lock lock = this.registry.obtain("foo");

		assertThat(lock.tryLock()).isTrue();
		try {
			registry.renewLock("foo");
		}
		finally {
			lock.unlock();
		}
	}

	@Test
	void testLockRenewLockNotOwned() {
		this.registry.obtain("foo");

		assertThatExceptionOfType(IllegalMonitorStateException.class)
				.isThrownBy(() -> registry.renewLock("foo"));
	}

	@Test
	void testLockRenewWithCustomTtl() throws InterruptedException {
		DefaultLockRepository clientOfAnotherProcess = new DefaultLockRepository(dataSource);
		clientOfAnotherProcess.setApplicationContext(this.context);
		clientOfAnotherProcess.afterPropertiesSet();
		clientOfAnotherProcess.afterSingletonsInstantiated();
		JdbcLockRegistry registryOfAnotherProcess = new JdbcLockRegistry(clientOfAnotherProcess, Duration.ofMillis(100));
		final DistributedLock lock = this.registry.obtain("foo");
		final Lock lockOfAnotherProcess = registryOfAnotherProcess.obtain("foo");

		assertThat(lock.tryLock(Duration.ofMillis(100), Duration.ofMillis(100))).isTrue();
		try {
			registry.renewLock("foo", Duration.ofSeconds(2));
			Thread.sleep(110);
			assertThat(lockOfAnotherProcess.tryLock(100, TimeUnit.MILLISECONDS)).isFalse();
		}
		finally {
			Assertions.assertDoesNotThrow(lock::unlock);
		}
	}

	@Test
	void concurrentObtainCapacityTest() throws InterruptedException {
		final int keyCnt = 500;
		final int capacityCnt = 179;
		final int threadCnt = 4;

		final CountDownLatch countDownLatch = new CountDownLatch(threadCnt);
		registry.setCacheCapacity(capacityCnt);
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCnt);

		for (int i = 0; i < keyCnt; i++) {
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
		assertThat(getRegistryLocks(registry)).hasSize(capacityCnt);

		registry.expireUnusedOlderThan(-1000);
		assertThat(getRegistryLocks(registry)).isEmpty();
	}

	@Test
	void concurrentObtainRemoveOrderTest() throws InterruptedException {
		final int threadCnt = 2;
		final int dummyLockCnt = 3;

		final CountDownLatch countDownLatch = new CountDownLatch(threadCnt);
		registry.setCacheCapacity(threadCnt);
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCnt);
		final Queue<String> remainLockCheckQueue = new LinkedBlockingQueue<>();

		//Removed due to capcity limit
		for (int i = 0; i < dummyLockCnt; i++) {
			Lock obtainLock0 = registry.obtain("foo:" + i);
			obtainLock0.lock();
			obtainLock0.unlock();
		}

		for (int i = dummyLockCnt; i < threadCnt + dummyLockCnt; i++) {
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
				remainLockCheckQueue.offer(toUUID(keyId));
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[0]));
	}

	@Test
	void concurrentObtainAccessRemoveOrderTest() throws InterruptedException {
		final int threadCnt = 2;
		final int dummyLockCnt = 3;

		final int CAPACITY_CNT = threadCnt + 1;
		final String REMAIN_DUMMY_LOCK_KEY = "foo:1";

		final CountDownLatch countDownLatch = new CountDownLatch(threadCnt);
		registry.setCacheCapacity(CAPACITY_CNT);
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCnt);
		final Queue<String> remainLockCheckQueue = new LinkedBlockingQueue<>();

		//Removed due to capcity limit
		for (int i = 0; i < dummyLockCnt; i++) {
			Lock obtainLock0 = registry.obtain("foo:" + i);
			obtainLock0.lock();
			obtainLock0.unlock();
		}

		Lock obtainLock0 = registry.obtain(REMAIN_DUMMY_LOCK_KEY);
		obtainLock0.lock();
		obtainLock0.unlock();
		remainLockCheckQueue.offer(toUUID(REMAIN_DUMMY_LOCK_KEY));

		for (int i = dummyLockCnt; i < threadCnt + dummyLockCnt; i++) {
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
				remainLockCheckQueue.offer(toUUID(keyId));
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[remainLockCheckQueue.size()]));
	}

	@Test
	void setCapacityTest() {
		final int capacityCnt = 4;
		registry.setCacheCapacity(capacityCnt);

		registry.obtain("foo:1");
		registry.obtain("foo:2");
		registry.obtain("foo:3");

		//capacity 4->3
		registry.setCacheCapacity(capacityCnt - 1);

		registry.obtain("foo:4");

		assertThat(getRegistryLocks(registry)).hasSize(3);
		assertThat(getRegistryLocks(registry)).containsKeys(toUUID("foo:2"),
				toUUID("foo:3"),
				toUUID("foo:4"));

		//capacity 3->4
		registry.setCacheCapacity(capacityCnt);
		registry.obtain("foo:5");
		assertThat(getRegistryLocks(registry)).hasSize(4);
		assertThat(getRegistryLocks(registry)).containsKeys(toUUID("foo:3"),
				toUUID("foo:4"),
				toUUID("foo:5"));
	}

	@Test
	void noTableThrowsExceptionOnStart() {
		try (TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext()) {
			DefaultLockRepository client = new DefaultLockRepository(this.dataSource);
			client.setPrefix("TEST_");
			client.setTransactionManager(this.transactionManager);
			testApplicationContext.registerBean("client", client);
			assertThatExceptionOfType(ApplicationContextException.class)
					.isThrownBy(testApplicationContext::refresh)
					.withRootCauseExactlyInstanceOf(JdbcSQLSyntaxErrorException.class)
					.withStackTraceContaining("Table \"TEST_LOCK\" not found");
		}
	}

	@Test
	void testUnlockAfterLockStatusHasBeenExpiredAndLockHasBeenAcquiredByAnotherProcess() throws Exception {
		Duration ttl = Duration.ofMillis(100);
		DefaultLockRepository client1 = new DefaultLockRepository(dataSource);
		client1.setApplicationContext(this.context);
		client1.afterPropertiesSet();
		client1.afterSingletonsInstantiated();
		DefaultLockRepository client2 = new DefaultLockRepository(dataSource);
		client2.setApplicationContext(this.context);
		client2.afterPropertiesSet();
		client2.afterSingletonsInstantiated();
		JdbcLockRegistry process1Registry = new JdbcLockRegistry(client1, ttl);
		JdbcLockRegistry process2Registry = new JdbcLockRegistry(client2, ttl);
		Lock lock1 = process1Registry.obtain("foo");
		Lock lock2 = process2Registry.obtain("foo");

		lock1.lock();
		Thread.sleep(ttl.toMillis());
		assertThat(lock2.tryLock()).isTrue();

		assertThatExceptionOfType(ConcurrentModificationException.class)
				.isThrownBy(lock1::unlock);
		lock2.unlock();
	}

	@Test
	void testUnlockAfterLockStatusHasBeenExpiredAndDeleted() throws Exception {
		DefaultLockRepository client = new DefaultLockRepository(dataSource);
		client.setApplicationContext(this.context);
		client.afterPropertiesSet();
		client.afterSingletonsInstantiated();
		JdbcLockRegistry registry = new JdbcLockRegistry(client, Duration.ofMillis(100));
		Lock lock = registry.obtain("foo");

		lock.lock();
		Thread.sleep(200);
		client.deleteExpired();

		assertThatExceptionOfType(ConcurrentModificationException.class)
				.isThrownBy(lock::unlock);
	}

	@Test
	void testPathForCanBeOverridden() {
		DefaultLockRepository client = new DefaultLockRepository(dataSource);
		client.setApplicationContext(this.context);
		client.afterPropertiesSet();
		client.afterSingletonsInstantiated();
		JdbcLockRegistry registry = new JdbcLockRegistry(client) {
			@Override
			protected String pathFor(String input) {
				return input;
			}
		};

		final String lockKey = "foo";
		Lock lock = registry.obtain(lockKey);
		String lockPath = TestUtils.getPropertyValue(lock, "path");

		assertThat(lockPath).isEqualTo(lockKey);
	}

	private static Map<String, Lock> getRegistryLocks(JdbcLockRegistry registry) {
		return TestUtils.getPropertyValue(registry, "locks");
	}

	private static String toUUID(String key) {
		return UUIDConverter.getUUID(key).toString();
	}

}
