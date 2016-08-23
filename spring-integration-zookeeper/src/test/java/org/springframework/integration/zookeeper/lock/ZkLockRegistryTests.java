/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.zookeeper.lock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.integration.zookeeper.lock.ZookeeperLockRegistry.KeyToPathStrategy;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @author Artem Bilan
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
				assertEquals(1, TestUtils.getPropertyValue(registry, "locks", Map.class).size());
			}
			finally {
				lock.unlock();
			}
		}

		Thread.sleep(10);
		registry.expireUnusedOlderThan(0);
		assertEquals(0, TestUtils.getPropertyValue(registry, "locks", Map.class).size());
		registry.destroy();
	}

	@Test
	public void testLockInterruptibly() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertEquals(1, TestUtils.getPropertyValue(registry, "locks", Map.class).size());
			}
			finally {
				lock.unlock();
			}
		}
		registry.destroy();
	}

	@Test
	public void testReentrantLock() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = registry.obtain("foo");
				assertSame(lock1, lock2);
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
				assertSame(lock1, lock2);
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
				assertNotSame(lock1, lock2);
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
		Future<Object> result = Executors.newSingleThreadExecutor().submit(new Callable<Object>() {

			@Override
			public Object call() throws Exception {
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
			}
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise, instanceOf(IllegalMonitorStateException.class));
		assertThat(((Exception) ise).getMessage(), containsString("You do not own"));
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
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
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
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock1.unlock();
		latch2.countDown();
		assertTrue(latch3.await(10, TimeUnit.SECONDS));
		assertTrue(locked.get());
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
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
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
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock1.unlock();
		latch2.countDown();
		assertTrue(latch3.await(10, TimeUnit.SECONDS));
		assertTrue(locked.get());
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
		Future<Object> result = Executors.newSingleThreadExecutor().submit(new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				try {
					lock.unlock();
				}
				catch (Exception e) {
					latch.countDown();
					return e.getCause();
				}
				return null;
			}
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock.unlock();
		Object imse = result.get(10, TimeUnit.SECONDS);
		assertThat(imse, instanceOf(IllegalMonitorStateException.class));
		assertThat(((Exception) imse).getMessage(), containsString("You do not own"));
		registry.destroy();
	}

	@Test
	public void testLockWithBoundedStrategy() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client, new KeyToPathStrategy() {

			@Override
			public String pathFor(String key) {
				return "/SpringIntegration-LockRegistry/singleLock";
			}

			@Override
			public boolean bounded() {
				return true;
			}
		});
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertEquals(1, TestUtils.getPropertyValue(registry, "locks", Map.class).size());
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
			assertThat(e.getMessage(), containsString("expiry is not supported"));
		}
		assertEquals(1, TestUtils.getPropertyValue(registry, "locks", Map.class).size());
		registry.destroy();
	}

	@Test
	public void voidLockFailsWhenServerDown() throws Exception {
		ZookeeperLockRegistry registry = new ZookeeperLockRegistry(this.client);

		Lock lock1 = registry.obtain("foo");
		lock1.lock();

		testingServer.stop();

		Lock lock2 = registry.obtain("bar");

		assertFalse("Should not have been able to lock with zookeeper server stopped!",
					lock2.tryLock(1, TimeUnit.SECONDS));

		testingServer.restart();

		assertTrue("Should have been able to lock with zookeeper server restarted!",
				lock2.tryLock(10, TimeUnit.SECONDS));

		assertTrue("Should have still held lock1", lock1.tryLock(1, TimeUnit.SECONDS));

		Lock lock3 = registry.obtain("foobar");

		assertTrue("Should have been able to a obtain new lock!", lock3.tryLock(1, TimeUnit.SECONDS));

		lock1.unlock();
		lock1.unlock();
		lock2.unlock();
		lock3.unlock();
		registry.destroy();
	}

}
