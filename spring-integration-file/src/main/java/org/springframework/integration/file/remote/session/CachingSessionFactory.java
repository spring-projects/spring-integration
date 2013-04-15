/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.util.SimplePool;

/**
 * A {@link SessionFactory} implementation that caches Sessions for reuse without
 * requiring reconnection each time the Session is retrieved from the factory.
 * This implementation wraps and delegates to a target SessionFactory instance.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class CachingSessionFactory<F> implements SessionFactory<F>, DisposableBean {

	private static final Log logger = LogFactory.getLog(CachingSessionFactory.class);

	private final SessionFactory<F> sessionFactory;

	private final SimplePool<Session<F>> pool;

	public CachingSessionFactory(SessionFactory<F> sessionFactory) {
		this(sessionFactory, 0);
	}

	public CachingSessionFactory(SessionFactory<F> sessionFactory, int sessionCacheSize) {
		this.sessionFactory = sessionFactory;
		this.pool = new SimplePool<Session<F>>(sessionCacheSize, new SimplePool.PoolItemCallback<Session<F>>() {
			public Session<F> createForPool() {
				return CachingSessionFactory.this.sessionFactory.getSession();
			}

			public boolean isStale(Session<F> session) {
				return !session.isOpen();
			}

			public void removedFromPool(Session<F> session) {
				session.close();
			}
		});
	}


	/**
	 * Sets the limit of how long to wait for a session to become available.
	 *
	 * @throws IllegalStateException if the wait expires prior to a Session becoming available.
	 */
	public void setSessionWaitTimeout(long sessionWaitTimeout) {
		this.pool.setWaitTimeout(sessionWaitTimeout);
	}

	public void setPoolSize(int poolSize) {
		this.pool.setPoolSize(poolSize);
	}

	public Session<F> getSession() {
		return new CachedSession(this.pool.getItem());
	}

	public void destroy() {
		this.pool.removeAllIdleItems();
	}


	private class CachedSession implements Session<F> {

		private final Session<F> targetSession;

		private boolean released;

		private CachedSession(Session<F> targetSession) {
			this.targetSession = targetSession;
		}

		public synchronized void close() {
			if (released) {
				if (logger.isDebugEnabled()){
					logger.debug("Session already released.");
				}
			}
			else {
				if (logger.isDebugEnabled()){
					logger.debug("Releasing Session back to the pool.");
				}
				pool.releaseItem(targetSession);
				released = true;
			}
		}

		public boolean remove(String path) throws IOException{
			return this.targetSession.remove(path);
		}

		public F[] list(String path) throws IOException{
			return this.targetSession.list(path);
		}

		public void read(String source, OutputStream os) throws IOException{
			this.targetSession.read(source, os);
		}

		public void write(InputStream inputStream, String destination) throws IOException{
			this.targetSession.write(inputStream, destination);
		}

		public boolean isOpen() {
			return this.targetSession.isOpen();
		}

		public void rename(String pathFrom, String pathTo) throws IOException {
			this.targetSession.rename(pathFrom, pathTo);
		}

		public boolean mkdir(String directory) throws IOException {
			return this.targetSession.mkdir(directory);
		}

		public boolean exists(String path) throws IOException{
			return this.targetSession.exists(path);
		}

		public String[] listNames(String path) throws IOException {
			return this.targetSession.listNames(path);
		}
	}

}
