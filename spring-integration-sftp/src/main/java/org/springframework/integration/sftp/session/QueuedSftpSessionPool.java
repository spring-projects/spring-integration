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

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * This approach - of having a SessionPool ({@link SftpSessionPool}) that has an
 * implementation of Queued*SessionPool ({@link QueuedSftpSessionPool}) - was
 * taken almost directly from the Spring Integration FTP adapter.
 *
 * @author Josh Long
 * @since 2.0
 */
public class QueuedSftpSessionPool implements SftpSessionPool, InitializingBean {

	public static final int DEFAULT_POOL_SIZE = 10;


	private volatile Queue<SftpSession> queue;

	private final SftpSessionFactory sftpSessionFactory;

	private final int maxPoolSize;


	public QueuedSftpSessionPool(SftpSessionFactory factory) {
		this(DEFAULT_POOL_SIZE, factory);
	}

	public QueuedSftpSessionPool(int maxPoolSize, SftpSessionFactory sessionFactory) {
		this.sftpSessionFactory = sessionFactory;
		this.maxPoolSize = maxPoolSize;
	}


	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.sftpSessionFactory, "sftpSessionFactory must not be null");
		Assert.isTrue(this.maxPoolSize > 0, "poolSize must be greater than 0");
		this.queue = new ArrayBlockingQueue<SftpSession>(this.maxPoolSize, true); // size, fairness to avoid starvation
	}

	public SftpSession getSession() throws Exception {
		SftpSession session = this.queue.poll();
		if (null == session) {
			session = this.sftpSessionFactory.getSession();
			if (this.queue.size() < this.maxPoolSize) {
				this.queue.add(session);
			}
		}
		if (null == session) {
			session = queue.poll();
		}
		return session;
	}

	public void release(SftpSession session) {
		if (queue.size() < maxPoolSize) {
			queue.add(session); // somehow one snuck in before <code>session</code> was finished!
		}
		else {
			dispose(session);
		}
	}

	private void dispose(SftpSession s) {
		if (s == null) {
			return;
		}
		if (queue.contains(s)) {
			//this should never happen, but if it does...
			queue.remove(s);
		}
		if ((s.getChannel() != null) && s.getChannel().isConnected()) {
			s.getChannel().disconnect();
		}
		if (s.getSession().isConnected()) {
			s.getSession().disconnect();
		}
	}

}
