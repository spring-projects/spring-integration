/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around a Semaphore that allows to create a potentially unlimited upper bound
 * to by used in buffers of messages (e.g. a QueueChannel or a MessageStore).
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public final class UpperBound {

	private final Semaphore semaphore;

	/**
	 * Create an UpperBound with the given capacity. If the given capacity is less than 1
	 * an infinite UpperBound is created.
	 *
	 * @param capacity The capacity.
	 */
	public UpperBound(int capacity) {
		this.semaphore = (capacity > 0) ? new Semaphore(capacity, true) : null;
	}

	public int availablePermits() {
		if (this.semaphore == null) {
			return Integer.MAX_VALUE;
		}
		return this.semaphore.availablePermits();
	}

	/**
	 * Acquires a permit from the underlying semaphore if this UpperBound is bounded and returns
	 * true if it succeeds within the given timeout. If the timeout is less than 0, it will block
	 * indefinitely.
	 *
	 * @param timeoutInMilliseconds The time to wait until a permit is available.
	 * @return true if a permit is acquired.
	 */
	public boolean tryAcquire(long timeoutInMilliseconds) {
		if (this.semaphore != null) {
			try {
				if (timeoutInMilliseconds < 0) {
					this.semaphore.acquire();
					return true;
				}
				return this.semaphore.tryAcquire(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	/**
	 * Releases one lock on the underlying semaphore. This is typically not done by the same Thread
	 * that acquired the lock, but by the thread that picked up the message.
	 */
	public void release() {
		if (this.semaphore != null) {
			this.semaphore.release();
		}
	}

	/**
	 * Releases several locks on the underlying semaphore. This is typically not done by the same Thread
	 * that acquired the lock, but by the thread that picked up the message.
	 *
	 * @param permits The number of permits to release.
	 */
	public void release(int permits) {
		if (this.semaphore != null) {
			this.semaphore.release(permits);
		}
	}

	@Override
	public String toString() {
		return super.toString() + "[Permits = " +
				(this.semaphore != null ? this.semaphore.availablePermits() : "UNLIMITED") + "]";
	}

}
