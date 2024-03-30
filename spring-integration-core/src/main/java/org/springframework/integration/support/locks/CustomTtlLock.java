/*
 * Copyright 2024 the original author or authors.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * A {@link Lock} implementing this interface supports the spring distributed locks with custom time-to-live value per lock
 *
 * @author Eddie Cho
 *
 * @since 6.3
 */
public interface CustomTtlLock extends Lock {

	/**
	 * Attempt to acquire a lock with a specific time-to-live
	 * @param time the maximum time to wait for the lock unit
	 * @param unit the time unit of the time argument
	 * @param customTtl the specific time-to-live for the lock status data
	 * @param customTtlUnit the time unit of the customTtl argument
	 * @return true if the lock was acquired and false if the waiting time elapsed before the lock was acquired
	 * @throws InterruptedException -
	 * if the current thread is interrupted while acquiring the lock (and interruption of lock acquisition is supported)
	 */
	boolean tryLock(long time, TimeUnit unit, long customTtl, TimeUnit customTtlUnit) throws InterruptedException;

	/**
	 * Attempt to acquire a lock with a specific time-to-live
	 * @param customTtl the specific time-to-live for the lock status data
	 * @param customTtlUnit the time unit of the customTtl argument
	 */
	void lock(long customTtl, TimeUnit customTtlUnit);
}
