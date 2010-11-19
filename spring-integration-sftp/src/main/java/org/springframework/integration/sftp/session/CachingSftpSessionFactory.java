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

package org.springframework.integration.sftp.session;

import java.io.InputStream;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

/**
 * This approach - of having a SessionPool ({@link SftpSessionPool}) that has an
 * implementation of a queued SessionPool ({@link CachingSftpSessionFactory}) - was
 * taken almost directly from the Spring Integration FTP adapter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class CachingSftpSessionFactory implements SftpSessionFactory, DisposableBean {

	private static Logger logger = Logger.getLogger(CachingSftpSessionFactory.class.getName());

	public static final int DEFAULT_POOL_SIZE = 10;


	private final Queue<SftpSession> queue;

	private final SimpleSftpSessionFactory sftpSessionFactory;

	private final int maxPoolSize;

	private final ReentrantLock lock = new ReentrantLock();


	public CachingSftpSessionFactory(SimpleSftpSessionFactory sessionFactory) {
		this(sessionFactory, DEFAULT_POOL_SIZE);
	}

	public CachingSftpSessionFactory(SimpleSftpSessionFactory sessionFactory, int maxPoolSize) {
		this.sftpSessionFactory = sessionFactory;
		this.maxPoolSize = maxPoolSize;
		this.queue = new ArrayBlockingQueue<SftpSession>(this.maxPoolSize, true);
	}


	public SftpSession getSession() {
		Assert.notNull(this.queue, "SftpSession is unavailable since the pool component is not started");
		this.lock.lock();
		try {
			SftpSession session = this.queue.poll();
			if (null == session) {
				session = sftpSessionFactory.getSession();
			}
			return (session != null) ? new PooledSftpSession(session) : null;
		} 
		finally {
			this.lock.unlock();
		}
		
	}

	public void destroy() {
		if (this.queue != null) {
			for (SftpSession sftpSession : this.queue) {
				this.destroySftpSession(sftpSession);
			}
		}
	}

	private void destroySftpSession(SftpSession sftpSession) {
		try {
			if (sftpSession != null) {
				sftpSession.disconnect();
			}	
		}
		catch (Throwable e) {
			// log and ignore
			logger.warning("Exception was thrown while destroying SftpSession. " + e);
		}
	}


	private class PooledSftpSession implements SftpSession {

		private final SftpSession targetSession;

		private PooledSftpSession(SftpSession targetSession) {
			this.targetSession = targetSession;
		}

		public void connect() {
			targetSession.connect();
		}

		public void disconnect() {
			if (queue.size() < maxPoolSize) {
				queue.add(targetSession);
			}
			else {
				targetSession.disconnect();
			}
		}

		public boolean exists(String path) {
			return this.targetSession.exists(path);
		}

		public boolean rm(String path) {
			return this.targetSession.rm(path);
		}

		public <F> Collection<F> ls(String path) {
			return this.targetSession.ls(path);
		}

		public InputStream get(String source) {
			return this.targetSession.get(source);
		}

		public void put(InputStream inputStream, String destination) {
			this.targetSession.put(inputStream, destination);
		}
	}

}
