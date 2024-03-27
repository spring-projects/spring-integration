/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link LockRegistry} which uses Masked Hashcode algorithm to obtain locks.
 * When an instance of this class is created and array of {@link Lock} objects is created. The length of
 * the array is based on the 'mask' parameter passed in the constructor. The default mask is 0xFF which will create
 * and array consisting of 256 {@link ReentrantLock} instances.
 * When the {@link #obtain(Object)} method is called with the lockKey (e.g., Object) the index of the {@link Lock}
 * is determined by masking the object's hashCode (e.g., {@code object.hashCode & mask}) and the {@link Lock} is returned.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1.1
 *
 */
public final class DefaultLockRegistry implements LockRegistry {

	private final Lock[] lockTable;

	private final int mask;

	/**
	 * Constructs a DefaultLockRegistry with the default
	 * mask 0xFF with 256 locks.
	 */
	public DefaultLockRegistry() {
		this(0xFF); // NOSONAR magic number
	}

	/**
	 * Constructs a DefaultLockRegistry with the supplied
	 * mask - the mask must have a value Math.pow(2, n) - 1 where n
	 * is 1 to 31, creating a hash of Math.pow(2, n) locks.
	 * <p> Examples:
	 * <ul>
	 * <li>0x3ff (1023) - 1024 locks</li>
	 * <li>0xfff (4095) - 4096 locks</li>
	 * </ul>
	 * @param mask The bit mask.
	 */
	public DefaultLockRegistry(int mask) {
		String bits = Integer.toBinaryString(mask);
		Assert.isTrue(bits.length() < 32 && (mask == 0 || bits.lastIndexOf('0') < bits.indexOf('1')), "Mask must be a power of 2 - 1"); // NOSONAR magic number
		this.mask = mask;
		int arraySize = this.mask + 1;
		this.lockTable = new ReentrantLock[arraySize];
		for (int i = 0; i < arraySize; i++) {
			this.lockTable[i] = new ReentrantLock();
		}
	}

	/**
	 * Obtains a lock by masking the lockKey's hashCode() with
	 * the mask and using the result as an index to the lock table.
	 * @param lockKey the object used to derive the lock index.
	 */
	@Override
	public Lock obtain(Object lockKey) {
		Assert.notNull(lockKey, "'lockKey' must not be null");
		Integer lockIndex = lockKey.hashCode() & this.mask;
		return this.lockTable[lockIndex];
	}

}
