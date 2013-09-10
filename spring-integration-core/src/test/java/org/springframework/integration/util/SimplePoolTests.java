/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SimplePoolTests {

	@Test
	public void testReuseAndStale() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		String s1 = pool.getItem();
		String s2 = pool.getItem();
		assertNotSame(s1, s2);
		pool.releaseItem(s1);
		String s3 = pool.getItem();
		assertSame(s1, s3);
		stale.set(true);
		pool.releaseItem(s3);
		s3 = pool.getItem();
		assertNotSame(s1, s3);
		assertFalse(strings.remove(s1));
		assertEquals(2, pool.getAllocatedCount());
	}

	@Test
	public void testOverCommitandResize() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		String s1 = pool.getItem();
		assertEquals(0, pool.getIdleCount());
		assertEquals(1, pool.getActiveCount());
		assertEquals(1, pool.getAllocatedCount());
		pool.releaseItem(s1);
		assertEquals(1, pool.getIdleCount());
		assertEquals(0, pool.getActiveCount());
		assertEquals(1, pool.getAllocatedCount());
		s1 = pool.getItem();
		assertEquals(0, pool.getIdleCount());
		assertEquals(1, pool.getActiveCount());
		assertEquals(1, pool.getAllocatedCount());
		String s2 = pool.getItem();
		assertNotSame(s1, s2);
		pool.setWaitTimeout(1);
		assertEquals(0, pool.getIdleCount());
		assertEquals(2, pool.getActiveCount());
		assertEquals(2, pool.getAllocatedCount());
		try {
			pool.getItem();
			fail("Expected exception");
		} catch (MessagingException e) {}

		// resize up
		pool.setPoolSize(4);

		assertEquals(0, pool.getIdleCount());
		assertEquals(2, pool.getActiveCount());
		assertEquals(2, pool.getAllocatedCount());
		String s3 = pool.getItem();
		String s4 = pool.getItem();
		assertEquals(0, pool.getIdleCount());
		assertEquals(4, pool.getActiveCount());
		assertEquals(4, pool.getAllocatedCount());
		pool.releaseItem(s4);
		assertEquals(1, pool.getIdleCount());
		assertEquals(3, pool.getActiveCount());
		assertEquals(4, pool.getAllocatedCount());

		// resize down
		pool.setPoolSize(2);

		assertEquals(0, pool.getIdleCount());
		assertEquals(3, pool.getActiveCount());
		assertEquals(3, pool.getPoolSize());
		assertEquals(3, pool.getAllocatedCount());
		pool.releaseItem(s3);
		assertEquals(0, pool.getIdleCount());
		assertEquals(2, pool.getActiveCount());
		assertEquals(2, pool.getPoolSize());
		assertEquals(2, pool.getAllocatedCount());
		assertEquals(2, strings.size());
		pool.releaseItem(s2);
		pool.releaseItem(s1);
		assertEquals(2, pool.getIdleCount());
		assertEquals(0, pool.getActiveCount());
		assertEquals(2, pool.getPoolSize());
		assertEquals(2, strings.size());
		assertEquals(2, pool.getAllocatedCount());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testForeignObject() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		pool.getItem();
		pool.releaseItem("Hello, world!");
	}

	@Test
	public void testDoubleReturn() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		Semaphore permits = TestUtils.getPropertyValue(pool, "permits", Semaphore.class);
		assertEquals(2, permits.availablePermits());
		String s1 = pool.getItem();
		assertEquals(1, permits.availablePermits());
		pool.releaseItem(s1);
		assertEquals(2, permits.availablePermits());
		pool.releaseItem(s1);
		assertEquals(2, permits.availablePermits());
	}


	private SimplePool<String> stringPool(int size, final Set<String> strings,
			final AtomicBoolean stale) {
		SimplePool<String> pool = new SimplePool<String>(size, new SimplePool.PoolItemCallback<String>() {
			private int i;
			public String createForPool() {
				String string = new String("String" + i++);
				strings.add(string);
				return string;
			}
			public boolean isStale(String item) {
				if (stale.get()) {
					strings.remove(item);
				}
				return stale.get();
			}
			public void removedFromPool(String item) {
				strings.remove(item);
			}
		});
		return pool;
	}
}
