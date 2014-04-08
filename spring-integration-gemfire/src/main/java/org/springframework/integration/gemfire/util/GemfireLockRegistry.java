/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gemfire.util;

import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.Scope;

/**
 * Implementation of {@link LockRegistry} providing a distributed lock using Gemfire.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class GemfireLockRegistry implements LockRegistry {

	public static final String LOCK_REGISTRY_REGION = "LockRegistry";

	private final Region<?, ?> region;

	public GemfireLockRegistry(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		this.region = cache.createRegionFactory().setScope(Scope.GLOBAL).create(LOCK_REGISTRY_REGION);
	}

	public GemfireLockRegistry(Region<?, ?> region) {
		Assert.notNull(region, "'region' must not be null");
		this.region = region;
	}

	@Override
	public Lock obtain(Object lockKey) {
		return this.region.getDistributedLock(lockKey);
	}

}
