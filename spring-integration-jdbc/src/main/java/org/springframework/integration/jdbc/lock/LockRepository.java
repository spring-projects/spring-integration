/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.lock;

import java.io.Closeable;

/**
 * Encapsulation of the SQL shunting that is needed for locks. A {@link JdbcLockRegistry}
 * needs a reference to a spring-managed (transactional) client service, so this component
 * has to be declared as a bean.
 *
 * @author Dave Syer
 * @author Alexandre Strubel
 * @author Artem Bilan
 *
 * @since 4.3
 */
public interface LockRepository extends Closeable {

	/**
	 * Check if a lock is held by this repository.
	 * @param lock the lock to check.
	 * @return acquired or not.
	 */
	boolean isAcquired(String lock);

	/**
	 * Remove a lock from this repository.
	 * @param lock the lock to remove.
	 */
	void delete(String lock);

	/**
	 * Remove all the expired locks.
	 */
	void deleteExpired();

	/**
	 * Acquire a lock for a key.
	 * @param lock the key for lock to acquire.
	 * @return acquired or not.
	 */
	boolean acquire(String lock);

	/**
	 * Renew the lease for a lock.
	 * @param lock the lock to renew.
	 * @return renewed or not.
	 */
	boolean renew(String lock);

	@Override
	void close();

}
