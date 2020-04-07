/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Implementation of {@link Pool} supporting dynamic resizing and a variable
 * timeout when attempting to obtain an item from the pool. Pool grows on
 * demand up to the limit.
 *
 * @author Gary Russell
 * @author Sergey Bogatyrev
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class SimplePool<T> implements Pool<T> {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final Semaphore permits = new Semaphore(0);

	private final AtomicInteger poolSize = new AtomicInteger();

	private final AtomicInteger targetPoolSize = new AtomicInteger();

	private long waitTimeout = Long.MAX_VALUE;

	private final BlockingQueue<T> available = new LinkedBlockingQueue<T>();

	private final Set<T> allocated = Collections.synchronizedSet(new HashSet<T>());

	private final Set<T> inUse = Collections.synchronizedSet(new HashSet<T>());

	private final PoolItemCallback<T> callback;

	/**
	 * Creates a SimplePool with a specific limit.
	 * @param poolSize The maximum number of items the pool supports.
	 * @param callback A {@link PoolItemCallback} implementation called during various
	 * pool operations.
	 */
	public SimplePool(int poolSize, PoolItemCallback<T> callback) {
		if (poolSize <= 0) {
			this.poolSize.set(Integer.MAX_VALUE);
			this.targetPoolSize.set(Integer.MAX_VALUE);
			this.permits.release(Integer.MAX_VALUE);
		}
		else {
			this.poolSize.set(poolSize);
			this.targetPoolSize.set(poolSize);
			this.permits.release(poolSize);
		}
		this.callback = callback;
	}

	/**
	 * Adjusts the current pool size. When reducing the pool size, attempts to
	 * remove the delta from the pool. If there are not enough unused items in
	 * the pool, the actual pool size will decrease to the specified size as in-use
	 * items are returned.
	 * @param poolSize The desired target pool size.
	 */
	public synchronized void setPoolSize(int poolSize) {
		int delta = poolSize - this.poolSize.get();
		this.targetPoolSize.addAndGet(delta);
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(String.format("Target pool size changed by %d, now %d", delta,
					this.targetPoolSize.get()));
		}
		if (delta > 0) {
			this.poolSize.addAndGet(delta);
			this.permits.release(delta);
		}
		else {
			while (delta < 0) {
				if (!this.permits.tryAcquire()) {
					break;
				}
				T item = this.available.poll();
				if (item != null) {
					doRemoveItem(item);
				}
				this.poolSize.decrementAndGet();
				delta++;
			}
		}
		if (delta < 0 && this.logger.isDebugEnabled()) {
			this.logger.debug(String.format("Pool is overcommitted by %d; items will be removed when returned",
					-delta));
		}
	}

	/**
	 * Returns the current size of the pool; may be greater than the target pool size
	 * if it was recently reduced and too many items were in use to allow the new size
	 * to be set.
	 */
	@Override
	public synchronized int getPoolSize() {
		return this.poolSize.get();
	}

	@Override
	public int getIdleCount() {
		return this.available.size();
	}

	@Override
	public int getActiveCount() {
		return this.inUse.size();
	}

	@Override
	public int getAllocatedCount() {
		return this.allocated.size();
	}

	/**
	 * Adjusts the wait timeout - the time for which getItem() will wait if no idle
	 * entries are available.
	 * <br>
	 * Default: infinity.
	 * @param waitTimeout The wait timeout in milliseconds.
	 */
	public void setWaitTimeout(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	/**
	 * Obtains an item from the pool; waits up to waitTime milliseconds (default infinity).
	 * @throws PoolItemNotAvailableException if no items become available in time.
	 */
	@Override
	public T getItem() {
		boolean permitted = false;
		try {
			try {
				permitted = this.permits.tryAcquire(this.waitTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new PoolItemNotAvailableException("Interrupted awaiting a pooled resource", e);
			}
			if (!permitted) {
				throw new PoolItemNotAvailableException("Timed out while waiting to acquire a pool entry.");
			}
			return doGetItem();
		}
		catch (Exception e) {
			if (permitted) {
				this.permits.release();
			}
			if (e instanceof PoolItemNotAvailableException) { // NOSONAR
				throw (PoolItemNotAvailableException) e;
			}
			throw new PoolItemNotAvailableException("Failed to obtain pooled item", e);
		}
	}

	private T doGetItem() {
		T item = this.available.poll();
		if (item != null && this.logger.isDebugEnabled()) {
			this.logger.debug("Obtained " + item + " from pool.");
		}
		if (item == null) {
			item = this.callback.createForPool();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Obtained new " + item + ".");
			}
			this.allocated.add(item);
		}
		else if (this.callback.isStale(item)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Received a stale item " + item + ", will attempt to get a new one.");
			}
			doRemoveItem(item);
			item = doGetItem();
		}
		this.inUse.add(item);
		return item;
	}

	/**
	 * Returns an item to the pool.
	 */
	@Override
	public synchronized void releaseItem(T item) {
		Assert.notNull(item, "Item cannot be null");
		Assert.isTrue(this.allocated.contains(item),
				"You can only release items that were obtained from the pool");
		if (this.inUse.contains(item)) {
			if (this.poolSize.get() > this.targetPoolSize.get()) {
				this.poolSize.decrementAndGet();
				doRemoveItem(item);
			}
			else {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Releasing " + item + " back to the pool");
				}
				this.available.add(item);
				this.inUse.remove(item);
				this.permits.release();
			}
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Ignoring release of " + item + " back to the pool - not in use");
			}
		}
	}

	@Override
	public synchronized void removeAllIdleItems() {
		T item;
		while ((item = this.available.poll()) != null) {
			doRemoveItem(item);
		}
	}

	private void doRemoveItem(T item) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Removing " + item + " from the pool");
		}
		this.allocated.remove(item);
		this.inUse.remove(item);
		this.callback.removedFromPool(item);
	}

	/**
	 * User of the pool provide an implementation of this interface; called during
	 * various pool operations.
	 *
	 */
	public interface PoolItemCallback<T> {

		/**
		 * Called by the pool when a new instance is required to populate the pool. Only
		 * called if no idle non-stale instances are available.
		 * @return The item.
		 */
		T createForPool();

		/**
		 * Called by the pool when an idle item is retrieved from the pool. Indicates
		 * whether that item is usable, or should be discarded. The pool takes no
		 * further action on a stale item, discards it, and attempts to find or create
		 * another item.
		 * @param item The item.
		 * @return true if the item should not be used.
		 */
		boolean isStale(T item);

		/**
		 * Called by the pool when an item is forcibly removed from the pool - for example
		 * when the pool size is reduced. The implementation should perform any cleanup
		 * necessary on the item, such as closing connections etc.
		 * @param item The item.
		 */
		void removedFromPool(T item);

	}

}
