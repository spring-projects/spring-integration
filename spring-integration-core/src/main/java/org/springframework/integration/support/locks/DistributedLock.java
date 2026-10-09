/*
 * Copyright 2025-present the original author or authors.
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
