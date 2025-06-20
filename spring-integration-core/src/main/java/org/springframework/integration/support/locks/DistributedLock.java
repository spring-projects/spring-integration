/*
 * Copyright 2025-present the original author or authors.
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
import java.util.concurrent.locks.Lock;

/**
 * A distributed {@link Lock} extension.
 *
 * @author Eddie Cho
 *
 * @since 7.0
 */
public interface DistributedLock extends Lock {

	/**
	 * Attempt to acquire a lock with a specific time-to-live
	 * @param ttl the specific time-to-live for the lock status data
	 */
	void lock(Duration ttl);

	/**
	 * Attempt to acquire a lock with a specific time-to-live
	 * @param waitTime the maximum time to wait for the lock
	 * @param ttl the specific time-to-live for the lock status data
	 * @return {@code true} if the lock was acquired and {@code false}
	 *         if the waiting time elapsed before the lock was acquired
	 * @throws InterruptedException if the current thread is interrupted
	 *         while acquiring the lock (and interruption of lock
	 *         acquisition is supported)
	 */
	boolean tryLock(Duration waitTime, Duration ttl) throws InterruptedException;
}
