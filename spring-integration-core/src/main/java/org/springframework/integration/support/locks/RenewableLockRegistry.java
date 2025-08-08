/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.locks;

/**
 * A {@link LockRegistry} implementing this interface supports the renewal
 * of the time to live of a lock.
 *
 * @author Alexandre Strubel
 * @author Artem Bilan
 *
 * @since 5.4
 */
public interface RenewableLockRegistry extends LockRegistry {

	/**
	 * Renew the time to live of the lock is associated with the parameter object.
	 * The lock must be held by the current thread
	 * @param lockKey The object with which the lock is associated.
	 */
	void renewLock(Object lockKey);

}
