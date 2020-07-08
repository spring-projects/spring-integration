/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @author Sergey Bogatyrev
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
		catch (PoolItemNotAvailableException e) {

		}

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

	@Test
	public void testForeignObject() {
		final Set<String> strings = new HashSet<String>();
		final AtomicBoolean stale = new AtomicBoolean();
		SimplePool<String> pool = stringPool(2, strings, stale);
		pool.getItem();
		assertThatIllegalArgumentException().isThrownBy(() -> pool.releaseItem("Hello, world!"));
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

	@Test
	public void testSizeUpdateIfNotAllocated() {
		SimplePool<String> pool = stringPool(10, new HashSet<>(), new AtomicBoolean());
		pool.setWaitTimeout(0);
		pool.setPoolSize(5);
		assertThat(pool.getPoolSize()).isEqualTo(5);

		// allocating all available items to check permits
		Set<String> allocatedItems = new HashSet<>();
		for (int i = 0; i < 5; i++) {
			allocatedItems.add(pool.getItem());
		}
		assertThat(allocatedItems).hasSize(5);

		// no more items can be allocated (indirect check of permits)
		assertThatExceptionOfType(PoolItemNotAvailableException.class).isThrownBy(() -> pool.getItem());
	}

	@Test
	public void testSizeUpdateIfPartiallyAllocated() {
		SimplePool<String> pool = stringPool(10, new HashSet<>(), new AtomicBoolean());
		pool.setWaitTimeout(0);
		List<String> allocated = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			allocated.add(pool.getItem());
		}

		// release only 2 items
		for (int i = 0; i < 2; i++) {
			pool.releaseItem(allocated.get(i));
		}

		// trying to reduce pool size
		pool.setPoolSize(5);

		// at this moment the actual pool size can be reduced only partially, because
		// only 2 items have been released, so 8 items are in use
		assertThat(pool.getPoolSize()).isEqualTo(8);
		assertThat(pool.getAllocatedCount()).isEqualTo(8);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(8);

		// releasing 3 items
		for (int i = 2; i < 5; i++) {
			pool.releaseItem(allocated.get(i));
		}

		// now pool size should be reduced
		assertThat(pool.getPoolSize()).isEqualTo(5);
		assertThat(pool.getAllocatedCount()).isEqualTo(5);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(5);

		// no more items can be allocated (indirect check of permits)
		assertThatExceptionOfType(PoolItemNotAvailableException.class).isThrownBy(() -> pool.getItem());
	}

	@Test
	public void testSizeUpdateIfFullyAllocated() {
		SimplePool<String> pool = stringPool(10, new HashSet<>(), new AtomicBoolean());
		pool.setWaitTimeout(0);
		List<String> allocated = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			allocated.add(pool.getItem());
		}

		// trying to reduce pool size
		pool.setPoolSize(5);

		// at this moment the actual pool size cannot be reduced - all in use
		assertThat(pool.getPoolSize()).isEqualTo(10);
		assertThat(pool.getAllocatedCount()).isEqualTo(10);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(10);

		// releasing 5 items
		for (int i = 0; i < 5; i++) {
			pool.releaseItem(allocated.get(i));
		}

		// now pool size should be reduced
		assertThat(pool.getPoolSize()).isEqualTo(5);
		assertThat(pool.getAllocatedCount()).isEqualTo(5);
		assertThat(pool.getIdleCount()).isEqualTo(0);
		assertThat(pool.getActiveCount()).isEqualTo(5);

		// no more items can be allocated (indirect check of permits)
		assertThatExceptionOfType(PoolItemNotAvailableException.class).isThrownBy(() -> pool.getItem());

		// releasing remaining items
		for (int i = 5; i < 10; i++) {
			pool.releaseItem(allocated.get(i));
		}

		assertThat(pool.getPoolSize()).isEqualTo(5);
		assertThat(pool.getAllocatedCount()).isEqualTo(5);
		assertThat(pool.getIdleCount()).isEqualTo(5);
		assertThat(pool.getActiveCount()).isEqualTo(0);
	}

	@Test
	void testClose() {
		SimplePool<String> pool = stringPool(10, new HashSet<>(), new AtomicBoolean());
		String item1 = pool.getItem();
		String item2 = pool.getItem();
		pool.releaseItem(item2);
		assertThat(pool.getAllocatedCount()).isEqualTo(2);
		pool.close();
		pool.releaseItem(item1);
		assertThat(pool.getAllocatedCount()).isEqualTo(0);
		assertThatIllegalStateException().isThrownBy(pool::getItem);
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
