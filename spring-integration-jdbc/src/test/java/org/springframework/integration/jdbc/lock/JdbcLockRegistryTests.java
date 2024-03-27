/*
 * Copyright 2016-2024 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
 *
 * @since 4.3
 */
@SpringJUnitConfig
@DirtiesContext
public class JdbcLockRegistryTests {

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
	public void testLock() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.registry.obtain("foo");
			lock.lock();
			try {
				assertThat(TestUtils.getPropertyValue(this.registry, "locks", Map.class).size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		this.registry.expireUnusedOlderThan(0);
		assertThat(TestUtils.getPropertyValue(this.registry, "locks", Map.class).size()).isEqualTo(0);
	}

	@Test
	public void testLockInterruptibly() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(TestUtils.getPropertyValue(this.registry, "locks", Map.class).size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	public void testReentrantLock() {
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
	public void testReentrantLockInterruptibly() throws Exception {
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
	public void testReentrantLockAfterExpiration() throws Exception {
		DefaultLockRepository client = new DefaultLockRepository(dataSource);
		client.setTimeToLive(1);
		client.setApplicationContext(this.context);
		client.afterPropertiesSet();
		client.afterSingletonsInstantiated();
		JdbcLockRegistry registry = new JdbcLockRegistry(client);
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
	public void testTwoLocks() throws Exception {
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
	public void testTwoThreadsSecondFailsToGetLock() throws Exception {
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
	public void testTwoThreads() throws Exception {
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
	public void testTwoThreadsDifferentRegistries() throws Exception {
		for (int i = 0; i < 100; i++) {

			final JdbcLockRegistry registry1 = new JdbcLockRegistry(this.client);
			final JdbcLockRegistry registry2 = new JdbcLockRegistry(this.client);
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
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
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
	public void testLockRenew() {
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
	public void testLockRenewLockNotOwned() {
		this.registry.obtain("foo");

		assertThatExceptionOfType(IllegalMonitorStateException.class)
				.isThrownBy(() -> registry.renewLock("foo"));
	}

	@Test
	public void concurrentObtainCapacityTest() throws InterruptedException {
		final int KEY_CNT = 500;
		final int CAPACITY_CNT = 179;
		final int THREAD_CNT = 4;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		registry.setCacheCapacity(CAPACITY_CNT);
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
		assertThat(getRegistryLocks(registry)).hasSize(CAPACITY_CNT);

		registry.expireUnusedOlderThan(-1000);
		assertThat(getRegistryLocks(registry)).isEmpty();
	}

	@Test
	public void concurrentObtainRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		registry.setCacheCapacity(CAPACITY_CNT);
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
	public void concurrentObtainAccessRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT + 1;
		final String REMAIN_DUMMY_LOCK_KEY = "foo:1";

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		registry.setCacheCapacity(CAPACITY_CNT);
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
		remainLockCheckQueue.offer(toUUID(REMAIN_DUMMY_LOCK_KEY));

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
	public void setCapacityTest() {
		final int CAPACITY_CNT = 4;
		registry.setCacheCapacity(CAPACITY_CNT);

		registry.obtain("foo:1");
		registry.obtain("foo:2");
		registry.obtain("foo:3");

		//capacity 4->3
		registry.setCacheCapacity(CAPACITY_CNT - 1);

		registry.obtain("foo:4");

		assertThat(getRegistryLocks(registry)).hasSize(3);
		assertThat(getRegistryLocks(registry)).containsKeys(toUUID("foo:2"),
				toUUID("foo:3"),
				toUUID("foo:4"));

		//capacity 3->4
		registry.setCacheCapacity(CAPACITY_CNT);
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

	@SuppressWarnings("unchecked")
	private static Map<String, Lock> getRegistryLocks(JdbcLockRegistry registry) {
		return TestUtils.getPropertyValue(registry, "locks", Map.class);
	}

	private static String toUUID(String key) {
		return UUIDConverter.getUUID(key).toString();
	}

}
