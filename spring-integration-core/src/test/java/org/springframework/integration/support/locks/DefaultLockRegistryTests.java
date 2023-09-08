/*
 * Copyright 2002-2023 the original author or authors.
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

import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.1.1
 *
 */
public class DefaultLockRegistryTests {

	@Test
	public void testBadMask() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultLockRegistry(4));
	}

	@Test
	public void testBadMaskOutOfRange() { // 32bits
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultLockRegistry(0xffffffff));
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

	@Test
	public void cyclicBarrierIsBrokenWhenExecutedConcurrentlyInLock() throws Exception {
		LockRegistry registry = new DefaultLockRegistry(1);

		CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
		CountDownLatch brokenBarrierLatch = new CountDownLatch(2);

		Runnable runnableLocked = () -> {
			try {
				registry.executeLocked("lockKey",
						() -> {
							try {
								cyclicBarrier.await(1, TimeUnit.SECONDS);
							}
							catch (BrokenBarrierException | TimeoutException e) {
								brokenBarrierLatch.countDown();
							}
						});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		ExecutorService executorService = Executors.newCachedThreadPool();

		executorService.execute(runnableLocked);
		executorService.execute(runnableLocked);

		assertThat(brokenBarrierLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void executeLockedIsTimedOutInOtherThread() throws Exception {
		LockRegistry registry = new DefaultLockRegistry(1);

		String lockKey = "lockKey";
		Duration waitLockDuration = Duration.ofMillis(100);

		CountDownLatch timeoutExceptionLatch = new CountDownLatch(1);
		AtomicReference<TimeoutException> exceptionAtomicReference = new AtomicReference<>();

		Runnable runnable = () -> {
			try {
				registry.executeLocked(lockKey, waitLockDuration, () -> Thread.sleep(200));
			}
			catch (TimeoutException e) {
				exceptionAtomicReference.set(e);
				timeoutExceptionLatch.countDown();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		ExecutorService executorService = Executors.newCachedThreadPool();

		executorService.execute(runnable);
		executorService.execute(runnable);

		assertThat(timeoutExceptionLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(exceptionAtomicReference.get())
				.hasMessage("The lock [%s] was not acquired in time: %s".formatted(lockKey, waitLockDuration));
	}

}

