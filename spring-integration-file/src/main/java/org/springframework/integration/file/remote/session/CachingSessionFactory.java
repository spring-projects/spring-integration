/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;

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
public class CachingSessionFactory implements SessionFactory, DisposableBean {

	private static final Log logger = LogFactory.getLog(CachingSessionFactory.class);

	public static final int DEFAULT_POOL_SIZE = 10;


	private final Queue<Session> queue;

	private final SessionFactory sessionFactory;

	private final int maxPoolSize;


	public CachingSessionFactory(SessionFactory sessionFactory) {
		this(sessionFactory, DEFAULT_POOL_SIZE);
	}

	public CachingSessionFactory(SessionFactory sessionFactory, int maxPoolSize) {
		this.sessionFactory = sessionFactory;
		this.maxPoolSize = maxPoolSize;
		this.queue = new ArrayBlockingQueue<Session>(this.maxPoolSize, true);
	}


	public Session getSession() {
		Session session = this.queue.poll();
		if (session == null || !session.isOpen()) {
			if (session != null && logger.isTraceEnabled()) {
				logger.trace("Located session in the pool but it is stale, will create new one.");
			}
			session = this.sessionFactory.getSession();
			if (logger.isTraceEnabled()) {
				logger.trace("Created new session");
			}
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Using session from the pool");
		}
		return new CachedSession(session);
	}

	public void destroy() {
		if (this.queue != null) {
			for (Session session : this.queue) {
				this.closeSession(session);
			}
		}
	}

	private void closeSession(Session session) {
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


	private class CachedSession implements Session {

		private final Session targetSession;

		private CachedSession(Session targetSession) {
			this.targetSession = targetSession;
		}

		public void close() {
			if (queue.size() < maxPoolSize) {
				if (logger.isTraceEnabled()) {
					logger.trace("Releasing target session back to the pool");
				}
				queue.add(targetSession);
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Disconnecting target session");
				}
				targetSession.close();
			}
		}

		public boolean remove(String path) throws IOException{
			return this.targetSession.remove(path);
		}

		public <F> F[] list(String path) throws IOException{
			return this.targetSession.<F>list(path);
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
	}

}
