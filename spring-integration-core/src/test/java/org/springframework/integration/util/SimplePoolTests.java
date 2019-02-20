/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

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
		assertThat(s2).isNotSameAs(s1);
		pool.releaseItem(s1);
		String s3 = pool.getItem();
		assertThat(s3).isSameAs(s1);
		stale.set(true);
		pool.releaseItem(s3);
		s3 = pool.getItem();
		assertThat(s3).isNotSameAs(s1);
		assertThat(strings.remove(s1)).isFalse();
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
	}

	@Test
	public void testOverCommitAndResize() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		String s1 = pool.getItem();
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(1);
		assertThat(pool.getAllocatedCount()).isEqualTo(1);
		pool.releaseItem(s1);
		assertThat(pool.getIdleCount()).isEqualTo(1);
		assertThat(pool.getActiveCount()).isEqualTo(0);
		assertThat(pool.getAllocatedCount()).isEqualTo(1);
		s1 = pool.getItem();
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(1);
		assertThat(pool.getAllocatedCount()).isEqualTo(1);
		String s2 = pool.getItem();
		assertThat(s2).isNotSameAs(s1);
		pool.setWaitTimeout(1);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(2);
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
		try {
			pool.getItem();
			fail("Expected exception");
		}
		catch (PoolItemNotAvailableException e) { }

		// resize up
		pool.setPoolSize(4);

		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(2);
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
		String s3 = pool.getItem();
		String s4 = pool.getItem();
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(4);
		assertThat(pool.getAllocatedCount()).isEqualTo(4);
		pool.releaseItem(s4);
		assertThat(pool.getIdleCount()).isEqualTo(1);
		assertThat(pool.getActiveCount()).isEqualTo(3);
		assertThat(pool.getAllocatedCount()).isEqualTo(4);

		// resize down
		pool.setPoolSize(2);

		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(3);
		assertThat(pool.getPoolSize()).isEqualTo(3);
		assertThat(pool.getAllocatedCount()).isEqualTo(3);
		pool.releaseItem(s3);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(2);
		assertThat(pool.getPoolSize()).isEqualTo(2);
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
		assertThat(strings.size()).isEqualTo(2);
		pool.releaseItem(s2);
		pool.releaseItem(s1);
		assertThat(pool.getIdleCount()).isEqualTo(2);
		assertThat(pool.getActiveCount()).isEqualTo(0);
		assertThat(pool.getPoolSize()).isEqualTo(2);
		assertThat(strings.size()).isEqualTo(2);
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
	}

	@Test(expected = IllegalArgumentException.class)
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
		assertThat(permits.availablePermits()).isEqualTo(2);
		String s1 = pool.getItem();
		assertThat(permits.availablePermits()).isEqualTo(1);
		pool.releaseItem(s1);
		assertThat(permits.availablePermits()).isEqualTo(2);
		pool.releaseItem(s1);
		assertThat(permits.availablePermits()).isEqualTo(2);
	}


	private SimplePool<String> stringPool(int size, final Set<String> strings,
			final AtomicBoolean stale) {
		SimplePool<String> pool = new SimplePool<String>(size, new SimplePool.PoolItemCallback<String>() {
			private int i;
			@Override
			public String createForPool() {
				String string = "String" + i++;
				strings.add(string);
				return string;
			}
			@Override
			public boolean isStale(String item) {
				if (stale.get()) {
					strings.remove(item);
				}
				return stale.get();
			}
			@Override
			public void removedFromPool(String item) {
				strings.remove(item);
			}
		});
		return pool;
	}

}
