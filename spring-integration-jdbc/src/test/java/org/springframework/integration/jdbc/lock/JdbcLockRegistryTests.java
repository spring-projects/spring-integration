/*
 * Copyright 2016-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Stefan Vassilev
 * @author Alexandre Strubel
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
		client.afterPropertiesSet();
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

}
