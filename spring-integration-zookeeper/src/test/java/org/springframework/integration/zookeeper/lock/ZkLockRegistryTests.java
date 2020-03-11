/*
 * Copyright 2015-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @author Artem Bilan\
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
				assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		registry.expireUnusedOlderThan(0);
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(0);
		registry.destroy();
	}

	@Test
	public void testLockInterruptibly() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
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
		Future<Object> result = Executors.newSingleThreadExecutor().submit(() -> {
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
		Executors.newSingleThreadExecutor().execute(() -> {
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
		Executors.newSingleThreadExecutor().execute(() -> {
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
	}

	@Test
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
		final ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		final Lock lock = registry.obtain("foo");
		lock.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = Executors.newSingleThreadExecutor().submit(() -> {
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
	}

	@Test
	public void testLockWithBoundedStrategy() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client,
				key -> "/SpringIntegration-LockRegistry/singleLock");
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

		Thread.sleep(10);
		try {
			registry.expireUnusedOlderThan(0);
			fail("expected exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage()).contains("expiry is not supported");
		}
		assertThat(TestUtils.getPropertyValue(registry, "locks", Map.class).size()).isEqualTo(1);
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

}
