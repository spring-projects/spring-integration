/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
