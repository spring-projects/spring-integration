/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import java.io.Serial;
import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.integration.support.locks.RenewableLockRegistry;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.Assert;

/**
 *
 * An {@link ExpirableLockRegistry} using a shared database to co-ordinate the locks.
 * Provides the same semantics as the
 * {@link org.springframework.integration.support.locks.DefaultLockRegistry}, but the
 * locks taken will be global, as long as the underlying database supports the
 * "serializable" isolation level in its transactions.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Kai Zimmermann
 * @author Bartosz Rempuszewski
 * @author Gary Russell
 * @author Alexandre Strubel
 * @author Stefan Vassilev
 * @author Olivier Hubaut
 * @author Fran Aranda
 * @author Unseok Kim
 * @author Christian Tzolov
 * @author Myeonghyeon Lee
 * @author Eddie Cho
 *
 * @since 4.3
 */
public class JdbcLockRegistry implements ExpirableLockRegistry<DistributedLock>, RenewableLockRegistry<DistributedLock> {

	private static final int DEFAULT_IDLE = 100;

	private static final int DEFAULT_CAPACITY = 100_000;

	private final Lock lock = new ReentrantLock();

	private final Map<String, JdbcLock> locks =
			new LinkedHashMap<>(16, 0.75F, true) {

				@Serial
				private static final long serialVersionUID = -8345579941944883141L;

				@Override
				protected boolean removeEldestEntry(Entry<String, JdbcLock> eldest) {
					return size() > JdbcLockRegistry.this.cacheCapacity;
				}

			};

	private final LockRepository client;

	private Duration idleBetweenTries = Duration.ofMillis(DEFAULT_IDLE);

	private int cacheCapacity = DEFAULT_CAPACITY;

	/**
	 * Default value for the time-to-live property.
	 * @since 7.0
	 */
	public static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

	private final Duration ttl;

	/**
	 * Construct an instance based on the provided {@link LockRepository}.
	 * @param client the {@link LockRepository} to rely on.
	 */
	public JdbcLockRegistry(LockRepository client) {
		this.client = client;
		this.ttl = DEFAULT_TTL;
	}

	/**
	 * Create a lock registry with the supplied lock expiration.
	 * @param client the {@link LockRepository} to rely on.
	 * @param expireAfter The expiration in {@link Duration}.
	 * @since 7.0
	 */
	public JdbcLockRegistry(LockRepository client, Duration expireAfter) {
		this.client = client;
		this.ttl = expireAfter;
	}

	/**
	 * Specify a {@link Duration} to sleep between lock record insert/update attempts.
	 * Defaults to 100 milliseconds.
	 * @param idleBetweenTries the {@link Duration} to sleep between insert/update attempts.
	 * @since 5.1.8
	 */
	public void setIdleBetweenTries(Duration idleBetweenTries) {
		Assert.notNull(idleBetweenTries, "'idleBetweenTries' must not be null");
		this.idleBetweenTries = idleBetweenTries;
	}

	/**
	 * Set the capacity of cached locks.
	 * @param cacheCapacity The capacity of cached lock, (default 100_000).
	 * @since 5.5.6
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	@Override
	public DistributedLock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		String path = pathFor((String) lockKey);
		this.lock.lock();
		try {
			return this.locks.computeIfAbsent(path, key -> new JdbcLock(this.client, this.idleBetweenTries, key));
		}
		finally {
			this.lock.unlock();
		}
	}

	protected String pathFor(String input) {
		return UUIDConverter.getUUID(input).toString();
	}

	@Override
	public void expireUnusedOlderThan(long age) {
		long now = System.currentTimeMillis();
		this.lock.lock();
		try {
			this.locks.entrySet()
					.removeIf(entry -> {
						JdbcLock lock = entry.getValue();
						return now - lock.getLastUsed() > age && !lock.isAcquiredInThisProcess();
					});
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void renewLock(Object lockKey) {
		this.renewLock(lockKey, this.ttl);
	}

	@Override
	public void renewLock(Object lockKey, Duration customTtl) {
		Assert.isInstanceOf(String.class, lockKey);
		String path = pathFor((String) lockKey);
		JdbcLock jdbcLock;
		this.lock.lock();
		try {
			jdbcLock = this.locks.get(path);
		}
		finally {
			this.lock.unlock();
		}

		if (jdbcLock == null) {
			throw new IllegalStateException("Could not found mutex at " + path);
		}
		if (!jdbcLock.renew(customTtl)) {
			throw new IllegalStateException("Could not renew mutex at " + path);
		}
	}

	private static Duration convertToDuration(long time, TimeUnit timeUnit) {
		long timeInMilliseconds = TimeUnit.MILLISECONDS.convert(time, timeUnit);
		return Duration.ofMillis(timeInMilliseconds);
	}

	private final class JdbcLock implements DistributedLock {

		private final LockRepository mutex;

		private final Duration idleBetweenTries;

		private final String path;

		private volatile long lastUsed = System.currentTimeMillis();

		private final ReentrantLock delegate = new ReentrantLock();

		JdbcLock(LockRepository client, Duration idleBetweenTries, String path) {
			this.mutex = client;
			this.idleBetweenTries = idleBetweenTries;
			this.path = path;
		}

		public long getLastUsed() {
			return this.lastUsed;
		}

		@Override
		public void lock() {
			lock(JdbcLockRegistry.this.ttl);
		}

		@Override
		public void lock(Duration ttl) {
			this.delegate.lock();
			while (true) {
				try {
					while (!doLock(ttl)) {
						Thread.sleep(this.idleBetweenTries.toMillis());
					}
					break;
				}
				catch (TransientDataAccessException | TransactionTimedOutException | TransactionSystemException e) {
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
					while (!doLock(JdbcLockRegistry.this.ttl)) {
						Thread.sleep(this.idleBetweenTries.toMillis());
						if (Thread.currentThread().isInterrupted()) {
							throw new InterruptedException();
						}
					}
					break;
				}
				catch (TransientDataAccessException | TransactionTimedOutException | TransactionSystemException e) {
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
			return tryLock(Duration.of(time, unit.toChronoUnit()), JdbcLockRegistry.this.ttl);
		}

		@Override
		public boolean tryLock(Duration waitTime, Duration ttl) throws InterruptedException {
			long now = System.currentTimeMillis();
			if (!this.delegate.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS)) {
				return false;
			}
			long expire = now + waitTime.toMillis();
			boolean acquired;
			while (true) {
				try {
					while (!(acquired = doLock(ttl)) && System.currentTimeMillis() < expire) { //NOSONAR
						Thread.sleep(this.idleBetweenTries.toMillis());
					}
					if (!acquired) {
						this.delegate.unlock();
					}
					return acquired;
				}
				catch (TransientDataAccessException | TransactionTimedOutException | TransactionSystemException e) {
					// try again
				}
				catch (Exception e) {
					this.delegate.unlock();
					rethrowAsLockException(e);
				}
			}
		}

		private boolean doLock(Duration ttl) {
			boolean acquired = this.mutex.acquire(this.path, ttl);
			if (acquired) {
				this.lastUsed = System.currentTimeMillis();
			}
			return acquired;
		}

		@Override
		public void unlock() {
			if (!this.delegate.isHeldByCurrentThread()) {
				throw new IllegalMonitorStateException("The current thread doesn't own mutex at " + this.path);
			}
			if (this.delegate.getHoldCount() > 1) {
				this.delegate.unlock();
				return;
			}
			try {
				while (true) {
					try {
						if (this.mutex.delete(this.path)) {
							return;
						}
						else {
							throw new ConcurrentModificationException("Lock was released in the store due to expiration. " +
									"The integrity of data protected by this lock may have been compromised.");
						}
					}
					catch (TransientDataAccessException | TransactionTimedOutException | TransactionSystemException e) {
						// try again
					}
					catch (ConcurrentModificationException e) {
						throw e;
					}
					catch (Exception e) {
						throw new DataAccessResourceFailureException("Failed to release mutex at " + this.path, e);
					}
				}
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
			return this.delegate.isLocked();
		}

		/**
		 * Renew the time-to-live of the distributed lock
		 * @return {@code true} if the lock's time-to-live was successfully renewed;
		 *         {@code false} if the time-to-live could not be renewed
		 */
		public boolean renew() {
			return renew(JdbcLockRegistry.this.ttl);
		}

		/**
		 * Renew the time-to-live of the distributed lock
		 * @param ttl the new time-to-live value for the lock status data
		 * @return {@code true} if the lock's time-to-live was successfully renewed;
		 *         {@code false} if the time-to-live could not be renewed
		 * @since 7.0
		 */
		public boolean renew(Duration ttl) {
			if (!this.delegate.isHeldByCurrentThread()) {
				throw new IllegalMonitorStateException("The current thread doesn't own mutex at " + this.path);
			}
			while (true) {
				try {
					boolean renewed = this.mutex.renew(this.path, ttl);
					if (renewed) {
						this.lastUsed = System.currentTimeMillis();
					}
					return renewed;
				}
				catch (TransientDataAccessException | TransactionTimedOutException | TransactionSystemException e) {
					// try again
				}
				catch (Exception e) {
					throw new DataAccessResourceFailureException("Failed to renew mutex at " + this.path, e);
				}
			}
		}

	}

}
