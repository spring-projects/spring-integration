/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.support.locks;

import java.util.concurrent.locks.Lock;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.1.1
 *
 */
public class DefaultLockRegistryTests {

	@Test(expected = IllegalArgumentException.class)
	public void testBadMask() {
		new DefaultLockRegistry(4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadMaskOutOfRange() { // 32bits
		new DefaultLockRegistry(0xffffffff);
	}

	@Test
	public void testSingleLockCreation() {
		LockRegistry registry = new DefaultLockRegistry(0);
		Lock a = registry.obtain(23);
		Lock b = registry.obtain(new Object());
		Lock c = registry.obtain("hello");
		assertThat(b).isSameAs(a);
		assertThat(c).isSameAs(a);
		assertThat(c).isSameAs(b);
	}

	@Test
	public void testSame() {
		LockRegistry registry = new DefaultLockRegistry();
		Lock lock1 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}
		});
		Lock lock2 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 256;
			}
		});
		assertThat(lock2).isSameAs(lock1);
	}

	@Test
	public void testDifferent() {
		LockRegistry registry = new DefaultLockRegistry();
		Lock lock1 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}
		});
		Lock lock2 = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 255;
			}
		});
		assertThat(lock2).isNotSameAs(lock1);
	}

	@Test
	public void testAllDifferentAndSame() {
		LockRegistry registry = new DefaultLockRegistry(3);
		Lock[] locks = new Lock[4];
		locks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}
		});
		locks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 1;
			}
		});
		locks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 2;
			}
		});
		locks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 3;
			}
		});
		for (int i = 0; i < 4; i++) {
			for (int j = 1; j < 4; j++) {
				if (i != j) {
					assertThat(locks[j]).isNotSameAs(locks[i]);
				}
			}
		}
		Lock[] moreLocks = new Lock[4];
		moreLocks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 0;
			}
		});
		moreLocks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 1;
			}
		});
		moreLocks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 2;
			}
		});
		moreLocks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 3;
			}
		});
		assertThat(moreLocks[0]).isSameAs(locks[0]);
		assertThat(moreLocks[1]).isSameAs(locks[1]);
		assertThat(moreLocks[2]).isSameAs(locks[2]);
		assertThat(moreLocks[3]).isSameAs(locks[3]);
		moreLocks[0] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 4;
			}
		});
		moreLocks[1] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 5;
			}
		});
		moreLocks[2] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 6;
			}
		});
		moreLocks[3] = registry.obtain(new Object() {

			@Override
			public int hashCode() {
				return 7;
			}
		});
		assertThat(moreLocks[0]).isSameAs(locks[0]);
		assertThat(moreLocks[1]).isSameAs(locks[1]);
		assertThat(moreLocks[2]).isSameAs(locks[2]);
		assertThat(moreLocks[3]).isSameAs(locks[3]);
	}

}
