/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.util.UpperBound;

/**
 * A {@link SessionFactory} implementation that caches Sessions for reuse without
 * requiring reconnection each time the Session is retrieved from the factory.
 * This implementation wraps and delegates to a target SessionFactory instance.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class CachingSessionFactory<F> implements SessionFactory<F>, DisposableBean {

	private static final Log logger = LogFactory.getLog(CachingSessionFactory.class);


	private volatile long sessionWaitTimeout = Integer.MAX_VALUE;

	private final LinkedBlockingQueue<Session<F>> queue = new LinkedBlockingQueue<Session<F>>();

	private final SessionFactory<F> sessionFactory;

	private final UpperBound sessionPermits;


	public CachingSessionFactory(SessionFactory<F> sessionFactory) {
		this(sessionFactory, 0);
	}
	
	public CachingSessionFactory(SessionFactory<F> sessionFactory, int sessionCacheSize) {
		this.sessionFactory = sessionFactory;
		this.sessionPermits = new UpperBound(sessionCacheSize);
	}


	/**
	 * Sets the limit of how long to wait for a session to become available. 
	 * 
	 * @throws {@link IllegalStateException} if the wait expires prior to a Session becoming available.
	 */
	public void setSessionWaitTimeout(long sessionWaitTimeout) {
		this.sessionWaitTimeout = sessionWaitTimeout;
	}

	public Session<F> getSession() {	
		boolean permitted = this.sessionPermits.tryAcquire(this.sessionWaitTimeout);
		if (!permitted) {
			throw new IllegalStateException("Timed out while waiting to aquire a Session.");
		}
		Session<F> session = this.doGetSession();
		return new CachedSession(session);
	}

	public void destroy() {
		if (this.queue != null) {
			for (Session<F> session : this.queue) {
				this.closeSession(session);
			}
		}
	}

	private Session<F> doGetSession() {
		Session<F> session = this.queue.poll();
		if (session != null && !session.isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Received a stale Session, will attempt to get a new one.");
			}
			return this.doGetSession();
		}
		else if (session == null){
			session = this.sessionFactory.getSession();
		}
		return session;
	}

	private void closeSession(Session<F> session) {
		try {
			if (session != null) {
				session.close();
			}
		}
		catch (Throwable e) {
			if (logger.isWarnEnabled()) {
				// log and ignore
				logger.warn("Exception was thrown while destroying Session. ", e);
			}
		}
	}


	private class CachedSession implements Session<F> {

		private final Session<F> targetSession;

		private CachedSession(Session<F> targetSession) {
			this.targetSession = targetSession;
		}

		public void close() {
			if (logger.isDebugEnabled()){
				logger.debug("Releasing Session back to the pool.");
			}
			queue.add(targetSession);
			sessionPermits.release();
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
	}

}
