/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support.locks;

import java.util.concurrent.locks.Lock;

/**
 * A {@link LockRegistry} implementing this interface supports the removal of aged locks
 * that are not currently locked.
 * @param <L> The expected class of the lock implementation
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface ExpirableLockRegistry<L extends Lock> extends LockRegistry<L> {

	/**
	 * Remove locks last acquired more than 'age' ago that are not currently locked.
	 * @param age the time since the lock was last obtained.
	 * @throws IllegalStateException if the registry configuration does not support this feature.
	 */
	void expireUnusedOlderThan(long age);

}
