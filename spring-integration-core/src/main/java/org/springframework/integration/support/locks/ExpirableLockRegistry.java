/*
 * Copyright 2015-2019 the original author or authors.
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
