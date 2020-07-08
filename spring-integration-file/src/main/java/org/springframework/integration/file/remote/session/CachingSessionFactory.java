/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.util.SimplePool;
import org.springframework.util.Assert;

/**
 * A {@link SessionFactory} implementation that caches Sessions for reuse without
 * requiring reconnection each time the Session is retrieved from the factory.
 * This implementation wraps and delegates to a target SessionFactory instance.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Alen Turkovic
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class CachingSessionFactory<F> implements SessionFactory<F>, DisposableBean {

	private static final Log LOGGER = LogFactory.getLog(CachingSessionFactory.class);

	private final SessionFactory<F> sessionFactory;

	private final SimplePool<Session<F>> pool;

	private final boolean isSharedSessionCapable;

	private boolean testSession;

	private volatile long sharedSessionEpoch;

	/**
	 * Create a CachingSessionFactory with an unlimited number of sessions.
	 * @param sessionFactory the underlying session factory.
	 */
	public CachingSessionFactory(SessionFactory<F> sessionFactory) {
		this(sessionFactory, 0);
	}

	/**
	 * Create a CachingSessionFactory with the specified session limit. By default, if
	 * no sessions are available in the cache, and the size limit has been reached,
	 * calling threads will block until a session is available.
	 * <p> Do not cache a {@link DelegatingSessionFactory}, cache each delegate therein instead.
	 * @see #setSessionWaitTimeout(long)
	 * @see #setPoolSize(int)
	 * @param sessionFactory The underlying session factory.
	 * @param sessionCacheSize The maximum cache size.
	 */
	public CachingSessionFactory(SessionFactory<F> sessionFactory, int sessionCacheSize) {
		Assert.isTrue(!(sessionFactory instanceof DelegatingSessionFactory),
				"'sessionFactory' cannot be a 'DelegatingSessionFactory'; cache each delegate instead");
		this.sessionFactory = sessionFactory;
		this.pool = new SimplePool<>(sessionCacheSize, new SimplePool.PoolItemCallback<Session<F>>() {
			@Override
			public Session<F> createForPool() {
				return CachingSessionFactory.this.sessionFactory.getSession();
			}

			@Override
			public boolean isStale(Session<F> session) {
				return CachingSessionFactory.this.testSession ? !session.test() : !session.isOpen();
			}

			@Override
			public void removedFromPool(Session<F> session) {
				session.close();
			}
		});
		this.isSharedSessionCapable = sessionFactory instanceof SharedSessionCapable;
	}


	/**
	 * Sets the limit of how long to wait for a session to become available.
	 * @param sessionWaitTimeout the session wait timeout.
	 * @throws IllegalStateException if the wait expires prior to a Session becoming available.
	 */
	public void setSessionWaitTimeout(long sessionWaitTimeout) {
		this.pool.setWaitTimeout(sessionWaitTimeout);
	}

	/**
	 * Modify the target session pool size; the actual pool size will adjust up/down
	 * to this size as and when sessions are requested or retrieved.
	 * @param poolSize The pool size.
	 */
	public void setPoolSize(int poolSize) {
		this.pool.setPoolSize(poolSize);
	}

	/**
	 * Set to true to test the session when checking one out from the cache.
	 * @param testSession true to test.
	 * @since 5.1
	 */
	public void setTestSession(boolean testSession) {
		this.testSession = testSession;
	}

	/**
	 * Get a session from the pool (or block if none available).
	 */
	@Override
	public Session<F> getSession() {
		return new CachedSession(this.pool.getItem(), this.sharedSessionEpoch);
	}

	/**
	 * Remove (close) any unused sessions in the pool.
	 */
	@Override
	public void destroy() {
		this.pool.close();
	}

	/**
	 * Clear the cache of sessions; also any in-use sessions will be closed when
	 * returned to the cache.
	 */
	public synchronized void resetCache() {
		LOGGER.debug("Cache reset; idle sessions will be removed, in-use sessions will be closed when returned");
		if (this.isSharedSessionCapable && ((SharedSessionCapable) this.sessionFactory).isSharedSession()) {
			((SharedSessionCapable) this.sessionFactory).resetSharedSession();
		}
		long epoch = System.nanoTime();
		/*
		 * Spin until we get a new value - nano precision but may be lower resolution.
		 * We reset the epoch AFTER resetting the shared session so there is no possibility
		 * of an "old" session being created in the new epoch. There is a slight possibility
		 * that a "new" session might appear in the old epoch and thus be closed when returned to
		 * the cache.
		 */
		while (epoch == this.sharedSessionEpoch) {
			epoch = System.nanoTime();
		}
		this.sharedSessionEpoch = epoch;
		this.pool.removeAllIdleItems();
	}

	public class CachedSession implements Session<F> { //NOSONAR must be final, but can't for mocking in tests

		private final Session<F> targetSession;

		private boolean released;

		private boolean dirty;

		/**
		 * The epoch in which this session was created.
		 */
		private final long sharedSessionEpoch;

		private CachedSession(Session<F> targetSession, long sharedSessionEpoch) {
			this.targetSession = targetSession;
			this.sharedSessionEpoch = sharedSessionEpoch;
		}

		@Override
		public synchronized void close() {
			if (this.released) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Session " + this.targetSession + " already released.");
				}
			}
			else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Releasing Session " + this.targetSession + " back to the pool.");
				}
				if (this.sharedSessionEpoch != CachingSessionFactory.this.sharedSessionEpoch) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Closing session " + this.targetSession + " after reset.");
					}
					this.targetSession.close();
				}
				else if (this.dirty) {
					this.targetSession.close();
				}
				if (this.targetSession.isOpen()) {
					try {
						this.targetSession.finalizeRaw();
					}
					catch (IOException e) {
						//No-op in this context
					}
				}
				CachingSessionFactory.this.pool.releaseItem(this.targetSession);
				this.released = true;
			}
		}

		@Override
		public boolean remove(String path) throws IOException {
			return this.targetSession.remove(path);
		}

		@Override
		public F[] list(String path) throws IOException {
			return this.targetSession.list(path);
		}

		@Override
		public void read(String source, OutputStream os) throws IOException {
			this.targetSession.read(source, os);
		}

		@Override
		public void write(InputStream inputStream, String destination) throws IOException {
			this.targetSession.write(inputStream, destination);
		}

		@Override
		public void append(InputStream inputStream, String destination) throws IOException {
			this.targetSession.append(inputStream, destination);
		}

		@Override
		public boolean isOpen() {
			return this.targetSession.isOpen();
		}

		@Override
		public void rename(String pathFrom, String pathTo) throws IOException {
			this.targetSession.rename(pathFrom, pathTo);
		}

		@Override
		public boolean mkdir(String directory) throws IOException {
			return this.targetSession.mkdir(directory);
		}

		@Override
		public boolean rmdir(String directory) throws IOException {
			return this.targetSession.rmdir(directory);
		}

		@Override
		public boolean exists(String path) throws IOException {
			return this.targetSession.exists(path);
		}

		@Override
		public String[] listNames(String path) throws IOException {
			return this.targetSession.listNames(path);
		}

		@Override
		public InputStream readRaw(String source) throws IOException {
			return this.targetSession.readRaw(source);
		}

		@Override
		public boolean finalizeRaw() throws IOException {
			return this.targetSession.finalizeRaw();
		}

		@Override
		public void dirty() {
			this.dirty = true;
		}

		@Override
		public Object getClientInstance() {
			return this.targetSession.getClientInstance();
		}

		@Override
		public String getHostPort() {
			return this.targetSession.getHostPort();
		}

	}

}
