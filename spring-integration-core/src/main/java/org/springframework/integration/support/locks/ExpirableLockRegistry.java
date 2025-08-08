/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support.locks;

/**
 * A {@link LockRegistry} implementing this interface supports the removal of aged locks
 * that are not currently locked.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface ExpirableLockRegistry extends LockRegistry {

	/**
	 * Remove locks last acquired more than 'age' ago that are not currently locked.
	 * @param age the time since the lock was last obtained.
	 * @throws IllegalStateException if the registry configuration does not support this feature.
	 */
	void expireUnusedOlderThan(long age);

}
