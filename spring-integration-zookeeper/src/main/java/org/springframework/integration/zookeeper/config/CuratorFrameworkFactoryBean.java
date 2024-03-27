/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.zookeeper.config;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * A Spring-friendly way to build a {@link CuratorFramework} and implementing {@link SmartLifecycle}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 4.2
 */
public class CuratorFrameworkFactoryBean implements FactoryBean<CuratorFramework>, SmartLifecycle {

	private final Lock lifecycleLock = new ReentrantLock();

	private final CuratorFramework client;

	/**
	 * @see SmartLifecycle
	 */
	private boolean autoStartup = true;

	/**
	 * @see SmartLifecycle
	 */
	private int phase = Integer.MIN_VALUE + 1000; // NOSONAR magic number

	/**
	 * @see SmartLifecycle
	 */
	private volatile boolean running;

	/**
	 * Construct an instance using the supplied connection string and using a default
	 * retry policy {@code new ExponentialBackoffRetry(1000, 3)}.
	 * @param connectionString list of servers to connect to
	 */
	public CuratorFrameworkFactoryBean(String connectionString) {
		this(connectionString, new ExponentialBackoffRetry(1000, 3)); // NOSONAR magic number
	}

	/**
	 * Construct an instance using the supplied connection string and retry policy.
	 * @param connectionString list of servers to connect to
	 * @param retryPolicy      the retry policy
	 */
	public CuratorFrameworkFactoryBean(String connectionString, RetryPolicy retryPolicy) {
		Assert.notNull(connectionString, "'connectionString' cannot be null");
		Assert.notNull(retryPolicy, "'retryPolicy' cannot be null");
		this.client = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * @param phase the phase
	 * @see SmartLifecycle
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * @param autoStartup true to automatically start
	 * @see SmartLifecycle
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				if (this.client != null) {
					this.client.start();
				}
				this.running = true;
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				CloseableUtils.closeQuietly(this.client);
				this.running = false;
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public CuratorFramework getObject() {
		return this.client;
	}

	@Override
	public Class<?> getObjectType() {
		return CuratorFramework.class;
	}

}
