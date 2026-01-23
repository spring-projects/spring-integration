/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.zookeeper.lock;

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

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Unseok Kim
 * @author Glenn Renfro
 *
 * @since 4.2
 *
 */
public class ZkLockRegistryTests extends ZookeeperTestSupport {

	@Test
	public void testLock() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertThat(TestUtils.<Map<?, ?>>getPropertyValue(registry, "locks").size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		registry.expireUnusedOlderThan(0);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(registry, "locks")).isEmpty();
		registry.destroy();
	}

	@Test
	public void testLockInterruptibly() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(TestUtils.<Map<?, ?>>getPropertyValue(registry, "locks").size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}
		registry.destroy();
	}

	@Test
	public void testReentrantLock() {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lock();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
		registry.destroy();
	}

	@Test
	public void testReentrantLockInterruptibly() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("foo");
				assertThat(lock2).isSameAs(lock1);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
		registry.destroy();
	}

	@Test
	public void testTwoLocks() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("bar");
				assertThat(lock2).isNotSameAs(lock1);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
		registry.destroy();
	}

	@Test
	public void testTwoThreadsSecondFailsToGetLock() throws Exception {
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
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
			catch (MessagingException e) {
				return e.getCause();
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) ise).getMessage()).contains("You do not own");
		registry.destroy();
		executorService.shutdown();
	}

	@Test
	public void testTwoThreads() throws Exception {
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		final Lock lock1 = registry.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			Lock lock2 = registry.obtain("foo");
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
		registry.destroy();
		executorService.shutdown();
	}

	@Test
	public void testTwoThreadsDifferentRegistries() throws Exception {
		final ZookeeperLockRegistry registry1 = new ZookeeperLockRegistry(this.client);
		final ZookeeperLockRegistry registry2 = new ZookeeperLockRegistry(this.client);
		final Lock lock1 = registry1.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
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
		registry1.destroy();
		registry2.destroy();
		executorService.shutdown();
	}

	@Test
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		final Lock lock = registry.obtain("foo");
		lock.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Object> result = executorService.submit(() -> {
			try {
				lock.unlock();
			}
			catch (Exception e) {
				latch.countDown();
				return e.getCause();
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock.unlock();
		Object imse = result.get(10, TimeUnit.SECONDS);
		assertThat(imse).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) imse).getMessage()).contains("You do not own");
		registry.destroy();
		executorService.shutdown();
	}

	@Test
	public void testLockWithBoundedStrategy() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client,
				key -> "/SpringIntegration-LockRegistry/singleLock");
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertThat(TestUtils.<Map<?, ?>>getPropertyValue(registry, "locks").size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		assertThatIllegalStateException()
				.isThrownBy(() -> registry.expireUnusedOlderThan(0))
				.withMessageContaining("expiry is not supported");
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(registry, "locks").size()).isEqualTo(1);
		registry.destroy();
	}

	@Test
	public void voidLockFailsWhenServerDown() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);

		Lock lock1 = registry.obtain("foo");
		lock1.lock();

		testingServer.stop();

		Lock lock2 = registry.obtain("bar");

		assertThat(lock2.tryLock(1, TimeUnit.SECONDS))
				.as("Should not have been able to lock with zookeeper server stopped!").isFalse();

		testingServer.restart();

		assertThat(lock2.tryLock(10, TimeUnit.SECONDS))
				.as("Should have been able to lock with zookeeper server restarted!").isTrue();

		assertThat(lock1.tryLock(1, TimeUnit.SECONDS)).as("Should have still held lock1").isTrue();

		Lock lock3 = registry.obtain("foobar");

		assertThat(lock3.tryLock(1, TimeUnit.SECONDS)).as("Should have been able to a obtain new lock!").isTrue();

		lock1.unlock();
		lock1.unlock();
		lock2.unlock();
		lock3.unlock();
		registry.destroy();
	}

	@Test
	public void testTryLock() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");

			int n = 0;
			while (!lock.tryLock() && n++ < 100) {
				Thread.sleep(100);
			}
			assertThat(n).isLessThan(100);

			lock.unlock();
		}

		registry.destroy();
	}

	@Test
	public void concurrentObtainCapacityTest() throws InterruptedException {
		final int KEY_CNT = 50;
		final int CAPACITY_CNT = 17;
		final int THREAD_CNT = 4;

		final CountDownLatch maincountDownLatch = new CountDownLatch(KEY_CNT);
		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
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
				maincountDownLatch.countDown();
				obtain.lock();
				obtain.unlock();
			});
		}
		executorService.shutdown();
		maincountDownLatch.await();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		//capacity limit test
		assertThat(getRegistryLocks(registry)).hasSize(CAPACITY_CNT);

		registry.expireUnusedOlderThan(-1000);
		assertThat(getRegistryLocks(registry)).isEmpty();
		registry.destroy();
	}

	@Test
	public void concurrentObtainRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT;

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
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
				remainLockCheckQueue.offer(toKey(keyId));
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[remainLockCheckQueue.size()]));
		registry.destroy();
	}

	@Test
	public void concurrentObtainAccessRemoveOrderTest() throws InterruptedException {
		final int THREAD_CNT = 2;
		final int DUMMY_LOCK_CNT = 3;

		final int CAPACITY_CNT = THREAD_CNT + 1;
		final String REMAIN_DUMMY_LOCK_KEY = "foo:1";

		final CountDownLatch countDownLatch = new CountDownLatch(THREAD_CNT);
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
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
		remainLockCheckQueue.offer(toKey(REMAIN_DUMMY_LOCK_KEY));

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
				remainLockCheckQueue.offer(toKey(keyId));
				Lock obtain = registry.obtain(keyId);
				obtain.lock();
				obtain.unlock();
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(getRegistryLocks(registry)).containsKeys(
				remainLockCheckQueue.toArray(new String[remainLockCheckQueue.size()]));
		registry.destroy();
	}

	@Test
	public void setCapacityTest() {
		final int CAPACITY_CNT = 4;
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		registry.setCacheCapacity(CAPACITY_CNT);

		registry.obtain("foo:1");
		registry.obtain("foo:2");
		registry.obtain("foo:3");

		//capacity 4->3
		registry.setCacheCapacity(CAPACITY_CNT - 1);

		registry.obtain("foo:4");

		assertThat(getRegistryLocks(registry)).hasSize(3);
		assertThat(getRegistryLocks(registry)).containsKeys(toKey("foo:2"), toKey("foo:3"), toKey("foo:4"));

		//capacity 3->4
		registry.setCacheCapacity(CAPACITY_CNT);
		registry.obtain("foo:5");
		assertThat(getRegistryLocks(registry)).hasSize(4);
		assertThat(getRegistryLocks(registry)).containsKeys(toKey("foo:3"), toKey("foo:4"), toKey("foo:5"));
		registry.destroy();
	}

	private static Map<String, Lock> getRegistryLocks(ZookeeperLockRegistry registry) {
		return TestUtils.getPropertyValue(registry, "locks");
	}

	private static String toKey(String path) {
		final String DEFAULT_ROOT = "/SpringIntegration-LockRegistry";
		return DEFAULT_ROOT + "/" + path;
	}

}
