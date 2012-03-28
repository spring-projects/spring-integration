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
package org.springframework.integration.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.Assert;
/**
 * Default implementation of {@link LockRegistry} which uses Masked Hashcode algorithm to obtain locks.
 * When an instance of this class is created and array of {@link Lock} objects is created. The length of
 * the array is based on the 'mask' parameter passed in the constructor. The default is 0xFF which will create
 * and array consisting of 256 {@link ReentrantLock} instances.
 * Once the {@link #obtain(Object)} method is called with the lockKey (e.g., Object) the index of the {@link Lock}
 * is determined by masking object's hashCode (e.g., object.hashCode & mask) and the {@link Lock} is returned.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1.1
 *
 */
public final class DefaultLockRegistry implements LockRegistry {

	private final Lock[] availableLocks;

	private final int mask;

	public DefaultLockRegistry(){
		this(0xFF);
	}

	public DefaultLockRegistry(int mask){
		String bits = Integer.toBinaryString(mask);
		Assert.isTrue(bits.lastIndexOf('0') < bits.indexOf('1'));
		this.mask = mask;
		int arraySize = this.mask+1;
		availableLocks = new ReentrantLock[arraySize];
		for (int i = 0; i < arraySize; i++) {
			availableLocks[i] = new ReentrantLock();
		}
	}

	public Lock obtain(Object lockKey) {
		Assert.notNull(lockKey, "'lockKey' must not be null");
		Integer lockIndex = lockKey.hashCode() & this.mask;
		return this.availableLocks[lockIndex];
	}
}