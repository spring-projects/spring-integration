/*
 * Copyright 2016-2024 the original author or authors.
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
 * @author Eddie Cho
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
	 * @return deleted or not.
	 */
	boolean delete(String lock);

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
