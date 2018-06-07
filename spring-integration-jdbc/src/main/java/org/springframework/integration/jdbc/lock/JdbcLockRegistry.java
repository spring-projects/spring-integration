/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.util.Assert;

/**
 *
 * A {@link LockRegistry} using a shared database to co-ordinate the locks. Provides the
 * same semantics as the {@link DefaultLockRegistry}, but the locks taken will be global,
 * as long as the underlying database supports the "serializable" isolation level in its
 * transactions.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Kai Zimmermann
 * @author Bartosz Rempuszewski
 *
 * @since 4.3
 */
public class JdbcLockRegistry implements ExpirableLockRegistry {

	private final Map<String, JdbcLock> locks = new ConcurrentHashMap<>();

	private final LockRepository client;

	public JdbcLockRegistry(LockRepository client) {
		this.client = client;
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		String path = pathFor((String) lockKey);
		return this.locks.computeIfAbsent(path, p -> new JdbcLock(this.client, p));
	}

	private String pathFor(String input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	@Override
	public void expireUnusedOlderThan(long age) {
		Iterator<Entry<String, JdbcLock>> iterator = this.locks.entrySet().iterator();
		long now = System.currentTimeMillis();
		while (iterator.hasNext()) {
			Entry<String, JdbcLock> entry = iterator.next();
			JdbcLock lock = entry.getValue();
			if (now - lock.getLastUsed() > age && !lock.isAcquiredInThisProcess()) {
				iterator.remove();
			}
		}
	}

	private static final class JdbcLock implements Lock {

		private final LockRepository mutex;

		private final String path;

		private volatile long lastUsed = System.currentTimeMillis();

		private final ReentrantLock delegate = new ReentrantLock();

		JdbcLock(LockRepository client, String path) {
			this.mutex = client;
			this.path = path;
		}

		public long getLastUsed() {
			return this.lastUsed;
		}

		@Override
		public void lock() {
			this.delegate.lock();
			while (true) {
				try {
					while (!doLock()) {
						Thread.sleep(100); //NOSONAR
					}
					break;
				}
				catch (TransientDataAccessException e) {
					// try again
				}
				catch (InterruptedException e) {
						/*
						 * This method must be uninterruptible so catch and ignore
						 * interrupts and only break out of the while loop when
						 * we get the lock.
						 */
				}
				catch (Exception e) {
					this.delegate.unlock();
					rethrowAsLockException(e);
				}
			}
		}

		private void rethrowAsLockException(Exception e) {
			throw new CannotAcquireLockException("Failed to lock mutex at " + this.path, e);
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			this.delegate.lockInterruptibly();
			while (true) {
				try {
					while (!doLock()) {
						Thread.sleep(100); //NOSONAR
						if (Thread.currentThread().isInterrupted()) {
							throw new InterruptedException();
						}
					}
					break;
				}
				catch (TransientDataAccessException e) {
					// try again
				}
				catch (InterruptedException ie) {
					this.delegate.unlock();
					Thread.currentThread().interrupt();
					throw ie;
				}
				catch (Exception e) {
					this.delegate.unlock();
					rethrowAsLockException(e);
				}
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
			long now = System.currentTimeMillis();
			if (!this.delegate.tryLock(time, unit)) {
				return false;
			}
			long expire = now + TimeUnit.MILLISECONDS.convert(time, unit);
			boolean acquired;
			while (true) {
				try {
					while (!(acquired = doLock()) && System.currentTimeMillis() < expire) { //NOSONAR
						Thread.sleep(100); //NOSONAR
					}
					if (!acquired) {
						this.delegate.unlock();
					}
					return acquired;
				}
				catch (TransientDataAccessException e) {
					// try again
				}
				catch (Exception e) {
					this.delegate.unlock();
					rethrowAsLockException(e);
				}
			}
		}

		private boolean doLock() {
			boolean acquired = this.mutex.acquire(this.path);
			if (acquired) {
				this.lastUsed = System.currentTimeMillis();
			}
			return acquired;
		}

		@Override
		public void unlock() {
			if (!this.delegate.isHeldByCurrentThread()) {
				throw new IllegalMonitorStateException("You do not own mutex at " + this.path);
			}
			if (this.delegate.getHoldCount() > 1) {
				this.delegate.unlock();
				return;
			}
			try {
				this.mutex.delete(this.path);
			}
			catch (Exception e) {
				throw new DataAccessResourceFailureException("Failed to release mutex at " + this.path, e);
			}
			finally {
				this.delegate.unlock();
			}
		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException("Conditions are not supported");
		}

		public boolean isAcquiredInThisProcess() {
			return this.mutex.isAcquired(this.path);
		}

	}

}
