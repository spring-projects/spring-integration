/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.zookeeper.lock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * {@link ExpirableLockRegistry} implementation using Zookeeper, or more specifically,
 * Curator {@link InterProcessMutex}.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class ZookeeperLockRegistry implements ExpirableLockRegistry {

	private static final String DEFAULT_ROOT = "/SpringIntegration-LockRegistry";

	private final CuratorFramework client;

	private final KeyToPathStrategy keyToPath;

	private final Map<String, ZkLock> locks = new HashMap<String, ZkLock>();

	private final boolean trackingTime;

	/**
	 * Construct a lock registry using the default {@link KeyToPathStrategy} which
	 * simple appends the key to '/SpringIntegration-LockRegistry/'.
	 * @param client the {@link CuratorFramework}.
	 */
	public ZookeeperLockRegistry(CuratorFramework client) {
		this(client, DEFAULT_ROOT);
	}

	/**
	 * Construct a lock registry using the default {@link KeyToPathStrategy} which
	 * simple appends the key to {@code '<root>/'}.
	 * @param client the {@link CuratorFramework}.
	 * @param root the path root (no trailing /).
	 */
	public ZookeeperLockRegistry(CuratorFramework client, String root) {
		this(client, new DefaultKeyToPathStrategy(root));
	}

	/**
	 * Construct a lock registry using the supplied {@link KeyToPathStrategy}.
	 * @param client the {@link CuratorFramework}.
	 * @param keyToPath the implementation of {@link KeyToPathStrategy}.
	 */
	public ZookeeperLockRegistry(CuratorFramework client, KeyToPathStrategy keyToPath) {
		Assert.notNull(client, "'client' cannot be null");
		Assert.notNull(client, "'keyToPath' cannot be null");
		this.client = client;
		this.keyToPath = keyToPath;
		this.trackingTime = !keyToPath.bounded();
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		String path = this.keyToPath.pathFor((String) lockKey);
		ZkLock lock = this.locks.get(path);
		if (lock == null) {
			synchronized (this.locks) {
				lock = this.locks.get(path);
				if (lock == null) {
					lock = new ZkLock(this.client, path);
					this.locks.put(path, lock);
				}
				if (this.trackingTime) {
					lock.setLastUsed(System.currentTimeMillis());
				}
			}
		}
		return lock;
	}

	/**
	 * Remove locks last acquired more than 'age' ago that are not currently locked.
	 * Expiry is not supported if the {@link KeyToPathStrategy} is bounded (returns a finite
	 * number of paths). With such a {@link KeyToPathStrategy}, the overhead of tracking when
	 * a lock is obtained is avoided.
	 * @param age the time since the lock was last obtained.
	 */
	@Override
	public void expireUnusedOlderThan(long age) {
		if (!this.trackingTime) {
			throw new IllegalStateException("Ths KeyToPathStrategy is bounded; expiry is not supported");
		}
		synchronized(this.locks) {
			Iterator<Entry<String, ZkLock>> iterator = this.locks.entrySet().iterator();
			long now = System.currentTimeMillis();
			while(iterator.hasNext()) {
				Entry<String, ZkLock> entry = iterator.next();
				ZkLock lock = entry.getValue();
				if (now - lock.getLastUsed() > age
						&& !lock.isAcquiredInThisProcess()) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Strategy to convert a lock key (e.g. aggregation correlation id) to a
	 * Zookeeper path.
	 *
	 */
	public interface KeyToPathStrategy {

		/**
		 * Return the path for the key.
		 * @param key the key.
		 * @return the path.
		 */
		String pathFor(String key);

		/**
		 * @return true if this strategy returns a bounded number of locks, removing
		 * the need for removing LRU locks.
		 */
		boolean bounded();

	}

	private static class DefaultKeyToPathStrategy implements KeyToPathStrategy {

		private final String root;

		public DefaultKeyToPathStrategy(String rootPath) {
			Assert.notNull(rootPath, "'rootPath' cannot be null");
			if (!rootPath.endsWith("/")) {
				this.root = rootPath + "/";
			}
			else {
				this.root = rootPath;
			}
		}

		@Override
		public String pathFor(String key) {
			return root + key;
		}

		@Override
		public boolean bounded() {
			return false;
		}

	}

	private static class ZkLock implements Lock {

		private final InterProcessMutex mutex;

		private final String path;

		private long lastUsed;

		public ZkLock(CuratorFramework client, String path) {
			this.mutex = new InterProcessMutex(client, path);
			this.path = path;
		}

		public long getLastUsed() {
			return this.lastUsed;
		}

		public void setLastUsed(long lastUsed) {
			this.lastUsed = lastUsed;
		}

		@Override
		public void lock() {
			try {
				this.mutex.acquire();
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to aquire mutex at " + this.path, e);
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			boolean locked = false;
			// this is a bit ugly, but...
			while (!locked) {
				locked = tryLock(1, TimeUnit.SECONDS);
			}

		}

		@Override
		public boolean tryLock() {
			try {
				return tryLock(0, TimeUnit.MICROSECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			try {
				return this.mutex.acquire(time, unit);
			}
			catch (Exception e) {
				throw new MessagingException("Failed to aquire mutex at " + this.path, e);
			}
		}

		@Override
		public void unlock() {
			try {
				this.mutex.release();
			}
			catch (Exception e) {
				throw new MessagingException("Failed to release mutex at " + this.path, e);
			}
		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException("Conditions are not supported");
		}

		public boolean isAcquiredInThisProcess() {
			return this.mutex.isAcquiredInThisProcess();
		}

	}

}
