/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.integration.hazelcast.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
public class HazelcastLockRegistryTests {

	private static final Config CONFIG = new Config();

	static {
		CONFIG.getCPSubsystemConfig().setCPMemberCount(0);
	}

	private static final HazelcastInstance instance = Hazelcast.newHazelcastInstance(CONFIG);

	@AfterAll
	public static void destroy() {
		HazelcastInstanceFactory.terminateAll();
	}

	@Test
	public void testLock() {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertThat(((FencedLock) lock).isLocked()).isTrue();
				assertThat(((FencedLock) lock).isLockedByCurrentThread()).isTrue();
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	public void testLockInterruptibly() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat(((FencedLock) lock).isLocked()).isTrue();
				assertThat(((FencedLock) lock).isLockedByCurrentThread()).isTrue();
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	public void testReentrantLock() {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
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
	}

	@Test
	public void testReentrantLockInterruptibly() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
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
	}

	@Test
	public void testTwoLocks() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
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
	}

	@Test
	public void testTwoThreadsSecondFailsToGetLock() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
		Lock lock1 = registry.obtain("foo");
		lock1.lockInterruptibly();
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Object> result = executorService.submit(() -> {
			Lock lock2 = registry.obtain("foo");
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
		assertThat(((Exception) ise).getMessage()).contains("Current thread is not owner of the lock!");
		executorService.shutdown();
	}

	@Test
	public void testTwoThreads() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
		Lock lock1 = registry.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
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
		executorService.shutdown();
	}

	@Test
	public void testTwoThreadsDifferentRegistries() throws Exception {
		HazelcastLockRegistry registry1 = new HazelcastLockRegistry(instance);
		HazelcastLockRegistry registry2 = new HazelcastLockRegistry(instance);
		Lock lock1 = registry1.obtain("foo");
		AtomicBoolean locked = new AtomicBoolean();
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
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
		executorService.shutdown();
	}

	@Test
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
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
				return e;
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock.unlock();
		Object imse = result.get(10, TimeUnit.SECONDS);
		assertThat(imse).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) imse).getMessage()).contains("Current thread is not owner of the lock!");
		executorService.shutdown();
	}

	@Test
	public void testTryLock() throws Exception {
		HazelcastLockRegistry registry = new HazelcastLockRegistry(instance);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");

			int n = 0;
			while (!lock.tryLock() && n++ < 100) {
				Thread.sleep(100);
			}
			assertThat(n).isLessThan(100);

			lock.unlock();
		}
	}

}
