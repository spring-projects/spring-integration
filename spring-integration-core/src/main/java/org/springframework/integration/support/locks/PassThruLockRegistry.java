/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.support.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * The {@link LockRegistry} implementation which has no effect. Mainly used in cases where locking itself must be conditional
 * but an extra IF statement would clutter the code.
 * For example. In the FILE module FileWritingMessageHandler is initialized with this instance of LockRegistry by default
 * since real locking is only required if its 'append' flag is set to true.
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public final class PassThruLockRegistry implements LockRegistry {

	public Lock obtain(Object lockKey) {
		return new Lock() {

			public void unlock() {
				// noop
			}

			public boolean tryLock(long time, TimeUnit unit)
					throws InterruptedException {
				return true;
			}

			public boolean tryLock() {
				return true;
			}

			public Condition newCondition() {
				throw new UnsupportedOperationException("This method is not supported for this implementation of Lock");
			}

			public void lockInterruptibly() throws InterruptedException {
				// noop
			}

			public void lock() {
				// noop
			}
		};
	}
}
