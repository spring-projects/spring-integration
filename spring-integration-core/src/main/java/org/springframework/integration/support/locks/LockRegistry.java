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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.util.CheckedCallable;
import org.springframework.integration.util.CheckedRunnable;

/**
 * Strategy for maintaining a registry of shared locks.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1.1
 */
@FunctionalInterface
public interface LockRegistry {

	/**
	 * Obtain the lock associated with the parameter object.
	 * @param lockKey The object with which the lock is associated.
	 * @return The associated lock.
	 */
	Lock obtain(Object lockKey);

	/**
	 * Perform a provided task when lock for the key is locked.
	 * @param lockKey the lock key to use
	 * @param runnable the {@link CheckedRunnable} to execute within a lock
	 * @param <E> type of exception runnable throws
	 * @throws InterruptedException from a lock operation
	 * @since 6.2
	 */
	default <E extends Throwable> void executeLocked(Object lockKey, CheckedRunnable<E> runnable)
			throws E, InterruptedException {

		executeLocked(lockKey,
				() -> {
					runnable.run();
					return null;
				});
	}

	/**
	 * Perform a provided task when lock for the key is locked.
	 * @param lockKey the lock key to use
	 * @param callable the {@link CheckedCallable} to execute within a lock
	 * @param <T> type of callable result
	 * @param <E> type of exception callable throws
	 * @return the result of callable
	 * @throws InterruptedException from a lock operation
	 * @since 6.2
	 */
	default <T, E extends Throwable> T executeLocked(Object lockKey, CheckedCallable<T, E> callable)
			throws E, InterruptedException {

		Lock lock = obtain(lockKey);
		lock.lockInterruptibly();
		try {
			return callable.call();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Perform a provided task when lock for the key is locked.
	 * @param lockKey the lock key to use
	 * @param waitLockDuration the {@link Duration} for {@link Lock#tryLock(long, TimeUnit)}
	 * @param runnable the {@link CheckedRunnable} to execute within a lock
	 * @param <E> type of exception runnable throws
	 * @throws InterruptedException from a lock operation
	 * @throws TimeoutException when {@link Lock#tryLock(long, TimeUnit)} has elapsed
	 * @since 6.2
	 */
	default <E extends Throwable> void executeLocked(Object lockKey, Duration waitLockDuration,
			CheckedRunnable<E> runnable) throws E, InterruptedException, TimeoutException {

		executeLocked(lockKey, waitLockDuration,
				() -> {
					runnable.run();
					return null;
				});
	}

	/**
	 * Perform a provided task when lock for the key is locked.
	 * @param lockKey the lock key to use
	 * @param waitLockDuration the {@link Duration} for {@link Lock#tryLock(long, TimeUnit)}
	 * @param callable the {@link CheckedCallable} to execute within a lock
	 * @param <E> type of exception callable throws
	 * @throws InterruptedException from a lock operation
	 * @throws TimeoutException when {@link Lock#tryLock(long, TimeUnit)} has elapsed
	 * @since 6.2
	 */
	default <T, E extends Throwable> T executeLocked(Object lockKey, Duration waitLockDuration,
			CheckedCallable<T, E> callable) throws E, InterruptedException, TimeoutException {

		Lock lock = obtain(lockKey);
		if (!lock.tryLock(waitLockDuration.toMillis(), TimeUnit.MILLISECONDS)) {
			throw new TimeoutException(
					"The lock [%s] was not acquired in time: %s".formatted(lockKey, waitLockDuration));
		}

		try {
			return callable.call();
		}
		finally {
			lock.unlock();
		}
	}

}
