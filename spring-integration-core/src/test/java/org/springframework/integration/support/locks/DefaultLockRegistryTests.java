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
package org.springframework.integration.support.locks;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.concurrent.locks.Lock;

import org.junit.Test;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.1.1
 *
 */
public class DefaultLockRegistryTests {

	@Test(expected=IllegalArgumentException.class)
	public void testBadMask() {
		new DefaultLockRegistry(4);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadMaskOutOfRange() {// 32bits
		new DefaultLockRegistry(0xffffffff);
	}

	@Test
	public void testSingleLockCreation() {
		LockRegistry registry = new DefaultLockRegistry(0);
		Lock a = registry.obtain(23);
		Lock b = registry.obtain(new Object());
		Lock c = registry.obtain("hello");
		assertSame(a, b);
		assertSame(a, c);
		assertSame(b, c);
	}

	@Test
	public void testSame() {
		LockRegistry registry = new DefaultLockRegistry();
		Lock lock1 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}});
		Lock lock2 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 256;
			}});
		assertSame(lock1, lock2);
	}

	@Test
	public void testDifferent() {
		LockRegistry registry = new DefaultLockRegistry();
		Lock lock1 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}});
		Lock lock2 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 255;
			}});
		assertNotSame(lock1, lock2);
	}

	@Test
	public void testAllDifferentAndSame() {
		LockRegistry registry = new DefaultLockRegistry(3);
		Lock[] locks = new Lock[4];
		locks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}});
		locks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 1;
			}});
		locks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 2;
			}});
		locks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 3;
			}});
		for (int i = 0; i < 4; i++) {
			for (int j = 1; j < 4; j++) {
				if (i != j) {
					assertNotSame(locks[i], locks[j]);
				}
			}
		}
		Lock[] moreLocks = new Lock[4];
		moreLocks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}});
		moreLocks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 1;
			}});
		moreLocks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 2;
			}});
		moreLocks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 3;
			}});
		assertSame(locks[0], moreLocks[0]);
		assertSame(locks[1], moreLocks[1]);
		assertSame(locks[2], moreLocks[2]);
		assertSame(locks[3], moreLocks[3]);
		moreLocks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 4;
			}});
		moreLocks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 5;
			}});
		moreLocks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 6;
			}});
		moreLocks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 7;
			}});
		assertSame(locks[0], moreLocks[0]);
		assertSame(locks[1], moreLocks[1]);
		assertSame(locks[2], moreLocks[2]);
		assertSame(locks[3], moreLocks[3]);
	}

}
