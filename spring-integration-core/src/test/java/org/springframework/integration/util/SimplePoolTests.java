/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.springframework.integration.MessagingException;

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
		pool.returnItem(s1);
		String s3 = pool.getItem();
		assertSame(s1, s3);
		stale.set(true);
		pool.returnItem(s3);
		s3 = pool.getItem();
		assertNotSame(s1, s3);
		assertFalse(strings.remove(s1));
	}

	@Test
	public void testOverCommitandResize() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		String s1 = pool.getItem();
		String s2 = pool.getItem();
		assertNotSame(s1, s2);
		pool.setWaitTimeout(1);
		assertEquals(0, pool.getIdleSize());
		assertEquals(2, pool.getInUseSize());
		try {
			pool.getItem();
			fail("Expected exception");
		} catch (MessagingException e) {}
		// resize up
		pool.setPoolSize(4);
		assertEquals(0, pool.getIdleSize());
		assertEquals(2, pool.getInUseSize());
		String s3 = pool.getItem();
		String s4 = pool.getItem();
		assertEquals(0, pool.getIdleSize());
		assertEquals(4, pool.getInUseSize());
		pool.returnItem(s4);
		assertEquals(1, pool.getIdleSize());
		assertEquals(3, pool.getInUseSize());
		// resize down
		pool.setPoolSize(2);
		assertEquals(0, pool.getIdleSize());
		assertEquals(3, pool.getInUseSize());
		assertEquals(3, pool.getPoolSize());
		pool.returnItem(s3);
		assertEquals(0, pool.getIdleSize());
		assertEquals(2, pool.getInUseSize());
		assertEquals(2, pool.getPoolSize());
		assertEquals(2, strings.size());
		pool.returnItem(s2);
		pool.returnItem(s1);
		assertEquals(2, pool.getIdleSize());
		assertEquals(0, pool.getInUseSize());
		assertEquals(2, pool.getPoolSize());
		assertEquals(2, strings.size());
	}

	private SimplePool<String> stringPool(int size, final Set<String> strings,
			final AtomicBoolean stale) {
		SimplePool<String> pool = new SimplePool<String>(size, new SimplePool.Callback<String>() {
			private int i;
			public String getNewItemForPool() {
				String string = new String("String" + i++);
				strings.add(string);
				return string;
			}
			public boolean isItemStale(String item) {
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
