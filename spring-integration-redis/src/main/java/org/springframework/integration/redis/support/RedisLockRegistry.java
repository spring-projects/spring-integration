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
package org.springframework.integration.redis.support;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.util.DefaultLockRegistry;
import org.springframework.integration.util.LockRegistry;
import org.springframework.util.Assert;

/**
 * Implementation of {@link LockRegistry} providing a distributed lock using Redis.
 * Locks are stored under the key {@code registryKey:lockKey}. Locks expire after
 * (default 60) seconds. Threads unlocking an
 * expired lock will get an {@link IllegalStateException}. This should be
 * considered as a critical error because it is possible the protected
 * resources were compromised.
 * <p>
 * Locks are reentrant.
 * <p>
 * <b>Note: This is not intended for low latency applications.</b> It is intended
 * for resource locking across multiple JVMs.
 * When a lock is released by a remote system, waiting threads may take up to 100ms
 * to acquire the lock.
 * A more performant version would need to get notifications from the Redis stores
 * of key changes. This is currently only available using the SYNC command.
 * <p>
 * This limitation will usually not apply when a lock is released within this registry,
 * unless another system takes the lock after the local lock is acquired here.
 * A {@link DefaultLockRegistry} is used internally to achieve this optimization.
 * <p>
 * {@link Condition}s are not supported.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public final class RedisLockRegistry implements LockRegistry {

	private static Log logger = LogFactory.getLog(LockRegistry.class);

	private static final byte[] hostName;

	private static final long DEFAULT_EXPIRE_AFTER = 60000;

	private final String registryKey;

	private final RedisTemplate<String, RedisLock> redisTemplate;

	private final ThreadLocal<List<RedisLock>> threadLocks = new ThreadLocal<List<RedisLock>>();

	private final long expireAfter;

	private final LockRegistry localRegistry = new DefaultLockRegistry();

	static {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			host = "unknownHost";
		}
		hostName = host.getBytes();
	}

	/**
	 * Constructs a lock registry with the default (60 second) lock expiration.
	 * @param connectionFactory The connection factory.
	 * @param registryKey The key prefix for locks.
	 */
	public RedisLockRegistry(RedisConnectionFactory connectionFactory, String registryKey) {
		this(connectionFactory, registryKey, DEFAULT_EXPIRE_AFTER);
	}

	/**
	 * Constructs a lock registry with the supplied lock expiration.
	 * @param connectionFactory The connection factory.
	 * @param registryKey The key prefix for locks.
	 * @param expireAfter The expiration in milliseconds.
	 */
	public RedisLockRegistry(RedisConnectionFactory connectionFactory, String registryKey, long expireAfter) {
		Assert.notNull(connectionFactory, "'connectionFactory' cannot be null");
		Assert.notNull(registryKey, "'registryKey' cannot be null");
		this.redisTemplate = new RedisTemplate<String, RedisLockRegistry.RedisLock>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new LockSerializer());
		this.redisTemplate.afterPropertiesSet();
		this.registryKey = registryKey;
		this.expireAfter = expireAfter;
	}

	@Override
	public Lock obtain(Object lockKey) {
		Assert.isInstanceOf(String.class, lockKey);
		List<RedisLock> locks = this.threadLocks.get();
		if (locks == null) {
			locks = new LinkedList<RedisLock>();
			this.threadLocks.set(locks);
		}
		RedisLock lock = null;
		for (RedisLock alock : locks) {
			if (alock.getLockKey().equals(lockKey)) {
				lock = alock;
				break;
			}
		}
		if (lock != null) {
			RedisLock lockInStore = RedisLockRegistry.this.redisTemplate
					.boundValueOps(this.registryKey + ":" + lockKey).get();
			if (lockInStore == null || !lock.equals(lockInStore)) {
				// lock has changed - must have expired
				lock = null;
				removeLockFromThreadLocal(locks, lock);
			}
		}
		if (lock == null) {
			lock = new RedisLock((String) lockKey);
			locks.add(lock);
		}
		return lock;
	}

	private void removeLockFromThreadLocal(List<RedisLock> locks, RedisLock lock) {
		Iterator<RedisLock> iterator = locks.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().equals(lock)) {
				iterator.remove();
				break;
			}
		}
	}

	public Collection<RedisLock> listLocks() {
		Set<String> keys = this.redisTemplate.keys(this.registryKey + ":*");
		List<RedisLock> locks = new ArrayList<RedisLock>(keys.size());
		for (String key : keys) {
			RedisLock lock = this.redisTemplate.boundValueOps(key).get();
			if (lock != null) {
				locks.add(lock);
			}
		}
		return locks;
	}

	public class RedisLock implements Lock {

		private final String lockKey;

		private long lockedAt;

		private Thread thread;

		private String threadName;

		private byte[] lockHost;

		private int reLock;

		private RedisLock(String lockKey) {
			this.lockKey = lockKey;
		}

		protected String getLockKey() {
			return lockKey;
		}

		protected long getLockedAt() {
			return lockedAt;
		}

		@Override
		public void lock() {
			Lock localLock = RedisLockRegistry.this.localRegistry.obtain(lockKey);
			localLock.lock();
			while (true) {
				try {
					while (!this.obtainLock()) {
						Thread.sleep(100);
					}
				}
				catch (InterruptedException e) {
				}
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			Lock localLock = RedisLockRegistry.this.localRegistry.obtain(lockKey);
			localLock.lockInterruptibly();
			while (!this.obtainLock()) {
				Thread.sleep(100);
			}
		}

		@Override
		public boolean tryLock() {
			Lock localLock = RedisLockRegistry.this.localRegistry.obtain(lockKey);
			if (!localLock.tryLock()) {
				return false;
			}
			return this.obtainLock();
		}

		private boolean obtainLock() {
			if (Thread.currentThread().equals(this.thread)) {
				this.reLock++;
				return true;
			}
			this.lockedAt = System.currentTimeMillis();
			this.thread = Thread.currentThread();
			Boolean success = RedisLockRegistry.this.redisTemplate.boundValueOps(
					constructLockKey()).setIfAbsent(this);
			if (!success) {
				this.lockedAt = 0;
				this.thread = null;
			}
			else {
				RedisLockRegistry.this.redisTemplate.expire(constructLockKey(),
						RedisLockRegistry.this.expireAfter, TimeUnit.MILLISECONDS);
				if (logger.isDebugEnabled()) {
					logger.debug("New lock; " + this.toString());
				}
			}
			return success;
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			long expire = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(time, unit);
			Lock localLock = RedisLockRegistry.this.localRegistry.obtain(lockKey);
			if (!localLock.tryLock(time, unit)) {
				return false;
			}
			boolean acquired = false;
			while (!(acquired = this.obtainLock()) && System.currentTimeMillis() < expire) {
				Thread.sleep(100);
			}
			return acquired;
		}

		@Override
		public void unlock() {
			if (!Thread.currentThread().equals(this.thread)) {
				if (this.thread == null) {
					throw new IllegalStateException("Lock is not locked; " + this.toString());
				}
				throw new IllegalStateException("Lock is owned by " + this.thread.getName() + "; " + this.toString());
			}
			if (this.reLock-- <= 0) {
				List<RedisLock> locks = RedisLockRegistry.this.threadLocks.get();
				if (locks != null) {
					removeLockFromThreadLocal(locks, this);
					if (locks.size() == 0) { // last lock for this thread
						RedisLockRegistry.this.threadLocks.remove();
					}
				}
				if (this.lockInRedisUnchanged()) {
					RedisLockRegistry.this.redisTemplate.delete(constructLockKey());
					if (logger.isDebugEnabled()) {
						logger.debug("Released lock; " + this.toString());
					}
				}
				this.thread = null;
				this.reLock = 0;
			}
			Lock localLock = RedisLockRegistry.this.localRegistry.obtain(lockKey);
			localLock.unlock();
		}

		private boolean lockInRedisUnchanged() {
			RedisLock lockInStore = RedisLockRegistry.this.redisTemplate.boundValueOps(
					constructLockKey()).get();
			if (lockInStore != null && this.equals(lockInStore)) {
				return true;
			}
			else {
				throw new IllegalStateException("Lock was released due to expiration; " + this.toString()
						+ (lockInStore == null ? "" : "; lock in store: " + lockInStore.toString()));
			}
		}

		private String constructLockKey() {
			return RedisLockRegistry.this.registryKey + ":" + this.lockKey;
		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException("Conditions are not supported");
		}

		@Override
		public String toString() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd@HH:mm:ss.SSS");
			return "RedisLock [lockKey=" + this.lockKey
					+ ",lockedAt=" + dateFormat.format(new Date(this.lockedAt))
					+ ", thread=" + (this.thread != null ? this.thread.getName() : this.threadName)
					+ ", lockHost=" + (this.lockHost != null ? new String(this.lockHost) :
						new String(RedisLockRegistry.hostName))
					+ "]";
		}

		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			return this.toString().equals(obj.toString());
		}

	}

	private class LockSerializer implements RedisSerializer<RedisLock> {

		@Override
		public byte[] serialize(RedisLock t) throws SerializationException {
			int hostLength = RedisLockRegistry.hostName.length;
			int keyLength = t.lockKey.length();
			int threadNameLength = t.thread.getName().length();
			byte[] value = new byte[1 + hostLength +
			                        1 + keyLength +
			                        1 + threadNameLength + 8];
			ByteBuffer buff = ByteBuffer.wrap(value);
			buff.put((byte) hostLength)
				.put(RedisLockRegistry.hostName)
				.put((byte) keyLength)
				.put(t.lockKey.getBytes())
				.put((byte) threadNameLength)
				.put(t.thread.getName().getBytes())
				.putLong(t.lockedAt);
			return value;
		}

		@Override
		public RedisLock deserialize(byte[] bytes) throws SerializationException {
			if (bytes == null) {
				return null;
			}
			ByteBuffer buff = ByteBuffer.wrap(bytes);
			byte[] host = new byte[buff.get()];
			buff.get(host);
			byte[] lockKey = new byte[buff.get()];
			buff.get(lockKey);
			byte[] threadName = new byte[buff.get()];
			buff.get(threadName);
			long lockedAt = buff.getLong();
			RedisLock lock = new RedisLock(new String(lockKey));
			lock.lockedAt = lockedAt;
			lock.lockHost = host;
			lock.threadName = new String(threadName);
			return lock;
		}

	}

}
