/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
