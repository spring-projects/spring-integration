/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.hazelcast.lock;

import java.util.concurrent.locks.Lock;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;

/**
 * A {@link LockRegistry} implementation Hazelcast distributed locks.
 *
 * @author Artem Bilan
 */
public class HazelcastLockRegistry implements LockRegistry {

	private final HazelcastInstance client;

	public HazelcastLockRegistry(HazelcastInstance hazelcastInstance) {
		Assert.notNull(hazelcastInstance, "'hazelcastInstance' must not be null");
		this.client = hazelcastInstance;
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		return this.client.getCPSubsystem().getLock((String) lockKey);
	}

}
