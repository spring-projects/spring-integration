/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.redis.util;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.rule.Log4jLevelAdjuster;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @author Konstantin Yakimov
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class RedisLockRegistryTests extends RedisAvailableTests {

	private final Log logger = LogFactory.getLog(getClass());

	private final String registryKey = UUID.randomUUID().toString();

	private final String registryKey2 = UUID.randomUUID().toString();

	@Rule
	public Log4jLevelAdjuster adjuster = new Log4jLevelAdjuster(Level.TRACE, "org.springframework.integration.redis");

	@Before
	@After
	public void setupShutDown() {
		RedisTemplate<String, ?> template = this.createTemplate();
		template.delete(this.registryKey + ":*");
		template.delete(this.registryKey2 + ":*");
	}

	private RedisTemplate<String, ?> createTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(this.getConnectionFactoryForTest());
		template.setKeySerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}

	@Test
	@RedisAvailable
	public void testLock() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lock();
			try {
				assertNotNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
			}
			finally {
				lock.unlock();
			}
		}
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testLockInterruptibly() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertNotNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
			}
			finally {
				lock.unlock();
			}
		}
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testReentrantLock() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = registry.obtain("foo");
				assertSame(lock1, lock2);
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
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testReentrantLockInterruptibly() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("foo");
				assertSame(lock1, lock2);
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
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testTwoLocks() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		for (int i = 0; i < 10; i++) {
			Lock lock1 = registry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = registry.obtain("bar");
				assertNotSame(lock1, lock2);
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
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsSecondFailsToGetLock() throws Exception {
		final RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
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
				catch (IllegalStateException ise) {
					return ise;
				}
				return null;
			}
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise, instanceOf(IllegalStateException.class));
		assertThat(((Exception) ise).getMessage(), containsString("Lock is not locked"));
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testTwoThreads() throws Exception {
		final RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		final Lock lock1 = registry.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertNotNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				Lock lock2 = registry.obtain("foo");
				try {
					latch1.countDown();
					lock2.lockInterruptibly();
					assertNotNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
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
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsDifferentRegistries() throws Exception {
		final RedisLockRegistry registry1 = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		final RedisLockRegistry registry2 = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		final Lock lock1 = registry1.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		assertNotNull(TestUtils.getPropertyValue(registry1, "hardThreadLocks", ThreadLocal.class).get());
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				Lock lock2 = registry2.obtain("foo");
				try {
					latch1.countDown();
					lock2.lockInterruptibly();
					assertNotNull(TestUtils.getPropertyValue(registry2, "hardThreadLocks", ThreadLocal.class).get());
					latch2.await(10, TimeUnit.SECONDS);
					locked.set(true);
					logger.debug("Locks in store: " + registry2.listLocks());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.error("Interrupted while locking: " + lock2, e);
				}
				finally {
					try {
						lock2.unlock();
						latch3.countDown();
					}
					catch (IllegalStateException e) {
						logger.error("Failed to unlock: " + lock2, e);
					}
				}
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock1.unlock();
		latch2.countDown();
		assertTrue(latch3.await(10, TimeUnit.SECONDS));
		assertTrue(locked.get());
		assertNull(TestUtils.getPropertyValue(registry1, "hardThreadLocks", ThreadLocal.class).get());
		assertNull(TestUtils.getPropertyValue(registry2, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testTwoThreadsWrongOneUnlocks() throws Exception {
		final RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
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
				catch (IllegalStateException ise) {
					latch.countDown();
					return ise;
				}
				return null;
			}
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertFalse(locked.get());
		lock.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise, instanceOf(IllegalStateException.class));
		assertThat(((Exception) ise).getMessage(), containsString("Lock is owned by"));
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testList() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey);
		Lock foo = registry.obtain("foo");
		foo.lockInterruptibly();
		Lock bar = registry.obtain("bar");
		bar.lockInterruptibly();
		Lock baz = registry.obtain("baz");
		baz.lockInterruptibly();
		Collection<Lock> locks = registry.listLocks();
		assertEquals(3, locks.size());
		foo.unlock();
		bar.unlock();
		baz.unlock();
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testExpireNoLockInStore() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey, 100);
		Lock foo = registry.obtain("foo");
		foo.lockInterruptibly();
		waitForExpire("foo");
		try {
			foo.unlock();
			fail("Expected exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Lock was released due to expiration"));
		}
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testExpireDuringSecondObtain() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey, 100);
		registry.setUseWeakReferences(true);
		Lock foo = registry.obtain("foo");
		foo.lockInterruptibly();
		waitForExpire("foo");
		Lock foo1 = registry.obtain("foo");
		assertNotSame(foo, foo1);

		try {
			foo.unlock();
			fail("IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Lock is not locked"));
		}
	}

	@Test
	@RedisAvailable
	public void testExpireNewLockInStore() throws Exception {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey, 100);
		Lock foo1 = registry.obtain("foo");
		foo1.lockInterruptibly();
		waitForExpire("foo");
		Lock foo2 = registry.obtain("foo");
		assertNotSame(foo1, foo2);
		foo2.lockInterruptibly();
		try {
			foo1.unlock();
			fail("Expected exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), containsString("Lock is not locked"));
		}
		foo2.unlock();
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testEquals() throws Exception {
		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();
		RedisLockRegistry registry1 = new RedisLockRegistry(connectionFactory, this.registryKey);
		registry1.setUseWeakReferences(true);
		RedisLockRegistry registry2 = new RedisLockRegistry(connectionFactory, this.registryKey);
		RedisLockRegistry registry3 = new RedisLockRegistry(connectionFactory, this.registryKey2);
		Lock lock1 = registry1.obtain("foo");
		Lock lock2 = registry1.obtain("foo");
		assertEquals(lock1, lock2);
		lock1.lock();
		lock2.lock();
		assertEquals(lock1, lock2);
		lock1.unlock();
		lock2.unlock();
		assertEquals(lock1, lock2);

		lock1 = registry1.obtain("foo");
		lock2 = registry2.obtain("foo");
		assertNotEquals(lock1, lock2);
		lock1.lock();
		assertFalse(lock2.tryLock());
		lock1.unlock();

		lock1 = registry1.obtain("foo");
		lock2 = registry3.obtain("foo");
		assertNotEquals(lock1, lock2);
		lock1.lock();
		lock2.lock();
		lock1.unlock();
		lock2.unlock();
	}

	@Test
	@RedisAvailable
	public void testThreadLocalListLeaks() {
		RedisLockRegistry registry = new RedisLockRegistry(this.getConnectionFactoryForTest(), this.registryKey, 100);
		registry.setUseWeakReferences(true);

		for (int i = 0; i < 10; i++) {
			registry.obtain("foo" + i);
		}
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			lock.lock();
		}
		assertEquals(10,
			((Collection<?>) TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get()).size());
		assertNull(TestUtils.getPropertyValue(registry, "weakThreadLocks", ThreadLocal.class).get());

		for (int i = 0; i < 10; i++) {
			Lock lock = registry.obtain("foo" + i);
			assertNotNull(TestUtils.getPropertyValue(lock, "thread", Thread.class));
			lock.unlock();
		}
		assertNull(TestUtils.getPropertyValue(registry, "hardThreadLocks", ThreadLocal.class).get());
	}

	@Test
	@RedisAvailable
	public void testExpireNotChanged() throws Exception {
		RedisConnectionFactory connectionFactory = this.getConnectionFactoryForTest();
		final RedisLockRegistry registry = new RedisLockRegistry(connectionFactory, this.registryKey, 10000);
		Lock lock = registry.obtain("foo");
		lock.lock();

		Long expire = getExpire(registry, "foo");

		Future<Object> result = Executors.newSingleThreadExecutor().submit(new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				Lock lock2 = registry.obtain("foo");
				assertFalse(lock2.tryLock());
				return null;
			}

		});
		result.get();
		assertEquals(expire, getExpire(registry, "foo"));
		lock.unlock();
	}

	private Long getExpire(RedisLockRegistry registry, String lockKey) {
		RedisTemplate<String, ?> template = this.createTemplate();
		String registryKey = TestUtils.getPropertyValue(registry, "registryKey", String.class);
		return template.getExpire(registryKey + ":" + lockKey);
	}

	private void waitForExpire(String key) throws Exception {
		RedisTemplate<String, ?> template = this.createTemplate();
		int n = 0;
		while (n++ < 100 && template.keys(this.registryKey + ":" + key).size() > 0) {
			Thread.sleep(100);
		}
		assertTrue(key + " key did not expire", n < 100);
	}

}
