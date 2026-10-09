/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.locks;

import java.time.Duration;
import java.util.concurrent.locks.Lock;

import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link LockRegistry} implementing this interface supports the renewal
 * of the time to live of a lock.
 * @param <L> The expected class of the lock implementation
 *
 * @author Alexandre Strubel
 * @author Artem Bilan
 * @author Youbin Wu
 * @author Eddie Cho
 *
 * @since 5.4
 */
public interface RenewableLockRegistry<L extends Lock> extends LockRegistry<L> {

	/**
	 * Renew the time to live of the lock is associated with the parameter object.
	 * The lock must be held by the current thread
	 * @param lockKey The object with which the lock is associated.
	 */
	void renewLock(Object lockKey);

	/**
	 * Renew the time to live of the lock is associated with the parameter object with a specific value.
	 * The lock must be held by the current thread
	 * @param lockKey The object with which the lock is associated.
	 * @param ttl the specific time-to-live for the lock status data
	 * @since 7.0
	 */
	void renewLock(Object lockKey, Duration ttl);

	/**
	 * Set the {@link TaskScheduler} to use for the renewal task.
	 * When renewalTaskScheduler is set, it will be used to periodically renew the lock to ensure that
	 * the lock does not expire while the thread is working.
	 * @param renewalTaskScheduler renew task scheduler
	 * @since 6.4
	 */
	default void setRenewalTaskScheduler(TaskScheduler renewalTaskScheduler) {
	}

}
