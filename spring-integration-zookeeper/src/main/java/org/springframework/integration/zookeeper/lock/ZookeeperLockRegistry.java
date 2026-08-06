/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.zookeeper.lock;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.KeeperException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * {@link ExpirableLockRegistry} implementation using Zookeeper, or more specifically,
 * Curator {@link InterProcessMutex}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Unseok Kim
 * @author Christian Tzolov
 * @author Glenn Renfro
 *
 * @since 4.2
 *
 */
public class ZookeeperLockRegistry implements ExpirableLockRegistry<Lock>, DisposableBean {

	private static final Log LOGGER = LogFactory.getLog(ZookeeperLockRegistry.class);

	private static final String DEFAULT_ROOT = "/SpringIntegration-LockRegistry";

	private final CuratorFramework client;

	private final KeyToPathStrategy keyToPath;

	private static final int DEFAULT_CAPACITY = 30_000;

	private final Lock locksLock = new ReentrantLock();

	private final Map<String, ZkLock> locks =
			new LinkedHashMap<>(16, 0.75F, true) {

				@Serial
				private static final long serialVersionUID = 7092378879531819061L;

				@Override
				protected boolean removeEldestEntry(Entry<String, ZkLock> eldest) {
					return size() > ZookeeperLockRegistry.this.cacheCapacity &&
							!eldest.getValue().isAcquiredInThisProcess();
				}
			};

	private final boolean trackingTime;

	private int cacheCapacity = DEFAULT_CAPACITY;

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

	/**
	 * Set an {@link AsyncTaskExecutor} to use when establishing (and testing) the
	 * connection with Zookeeper.
	 * @param mutexTaskExecutor the executor.
	 * @since 4.2.10
	 * @deprecated since 7.0.6 with no replacement.
	 * The connection state is now determined via a non-blocking
	 * {@link org.apache.curator.CuratorZookeeperClient#isConnected()}, so no executor
	 * is involved into locking anymore and this option has no effect.
	 */
	@Deprecated(since = "7.0.6", forRemoval = true)
	@SuppressWarnings("unused")
	public void setMutexTaskExecutor(AsyncTaskExecutor mutexTaskExecutor) {
		LOGGER.warn("The 'mutexTaskExecutor' is not used anymore and will be removed in a future release.");
	}

	/**
	 * Set the capacity of cached locks.
	 * @param cacheCapacity The capacity of cached lock, (default 30_000).
	 * @since 5.5.6
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		String path = this.keyToPath.pathFor((String) lockKey);
		ZkLock lock;
		this.locksLock.lock();
		try {
			lock = this.locks.computeIfAbsent(path, p -> new ZkLock(this.client, p));
		}
		finally {
			this.locksLock.unlock();
		}
		if (this.trackingTime) {
			lock.setLastUsed(System.currentTimeMillis());
		}
		return lock;
	}

	/**
	 * Remove locks last acquired more than 'age' ago that are not currently locked. Expiry is not supported if the
	 * {@link KeyToPathStrategy} is bounded (returns a finite number of paths). With such a {@link KeyToPathStrategy},
	 * the overhead of tracking when a lock is obtained is avoided.
	 * @param age the time since the lock was last obtained.
	 */
	@Override
	public void expireUnusedOlderThan(long age) {
		if (!this.trackingTime) {
			throw new IllegalStateException("Ths KeyToPathStrategy is bounded; expiry is not supported");
		}

		long now = System.currentTimeMillis();
		this.locksLock.lock();
		try {
			this.locks.entrySet()
					.removeIf(entry -> {
						ZkLock lock = entry.getValue();
						return now - lock.getLastUsed() > age && !lock.isAcquiredInThisProcess();
					});
		}
		finally {
			this.locksLock.unlock();
		}

	}

	/**
	 * No-op since version 7.0.6: this registry does not manage any resource of its own anymore.
	 * The {@link CuratorFramework} client is provided externally, therefore it has to be closed
	 * by the calling side as well.
	 */
	@Override
	public void destroy() {
	}

	/**
	 * Strategy to convert a lock key (e.g. aggregation correlation id) to a
	 * Zookeeper path.
	 *
	 */
	@FunctionalInterface
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
		default boolean bounded() {
			return true;
		}

	}

	private static final class DefaultKeyToPathStrategy implements KeyToPathStrategy {

		private final String root;

		DefaultKeyToPathStrategy(String rootPath) {
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
			return this.root + key;
		}

	}

	private static final class ZkLock implements Lock {

		private final CuratorFramework client;

		private final InterProcessMutex mutex;

		private final String path;

		private long lastUsed;

		ZkLock(CuratorFramework client, String path) {
			this.client = client;
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
				throw new IllegalStateException("Failed to acquire mutex at " + this.path, e);
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			// this is a bit ugly, but...
			while (!tryLock(1, TimeUnit.SECONDS)) {
				// The tryLock() above may return 'false' without blocking at all, e.g. when Zookeeper is not reachable.
				// Therefore, the interrupt status has to be checked explicitly
				// to avoid an endless, silent spin in this loop.
				if (Thread.interrupted()) {
					throw new InterruptedException("Interrupted while acquiring mutex at " + this.path);
				}
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Mutex at " + this.path + " is not acquired yet; retrying...");
				}
			}
		}

		@Override
		public boolean tryLock() {
			try {
				return tryLock(1, TimeUnit.SECONDS);
			}
			catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			try {
				long startTime = System.currentTimeMillis();
				long waitTime = unit.toMillis(time);

				// The non-blocking state check first; only an unconnected client is waited for,
				// interruptibly and within the requested time, to let a re-connect happen.
				if (!this.client.getZookeeperClient().isConnected() &&
						!this.client.blockUntilConnected((int) Math.min(waitTime, Integer.MAX_VALUE),
								TimeUnit.MILLISECONDS)) {

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("No Zookeeper connection to acquire mutex at " + this.path);
					}
					return false;
				}

				waitTime = Math.max(0, waitTime - (System.currentTimeMillis() - startTime));
				return this.mutex.acquire(waitTime, TimeUnit.MILLISECONDS);
			}
			catch (KeeperException.ConnectionLossException | KeeperException.SessionExpiredException |
					KeeperException.OperationTimeoutException e) {

				// The connection may be lost between the state check and the acquisition:
				// this is a `false` for the `Lock.tryLock()` contract, not an error.
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Lost the Zookeeper connection to acquire mutex at " + this.path, e);
				}
				return false;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
			catch (Exception e) {
				throw new MessagingException("Failed to acquire mutex at " + this.path, e);
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
