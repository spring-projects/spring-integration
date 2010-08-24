/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.sftp;

import org.springframework.beans.factory.InitializingBean;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * This approach - of having a SessionPool ({@link SftpSessionPool}) that has an
 * implementation of Queued*SessionPool ({@link QueuedSftpSessionPool}) - was
 * taken pretty directly from the incredibly good Spring IntegrationFTP adapter.
 *
 * @author Josh Long
 * @since 2.0
 */
public class QueuedSftpSessionPool implements SftpSessionPool, InitializingBean {
	public static final int DEFAULT_POOL_SIZE = 10;
	private Queue<SftpSession> queue;
	private final SftpSessionFactory sftpSessionFactory;
	private int maxPoolSize;

	public QueuedSftpSessionPool(SftpSessionFactory factory) {
		this(DEFAULT_POOL_SIZE, factory);
	}

	public QueuedSftpSessionPool(int maxPoolSize, SftpSessionFactory sessionFactory) {
		this.sftpSessionFactory = sessionFactory;
		this.maxPoolSize = maxPoolSize;
	}

	public void afterPropertiesSet() throws Exception {
		assert maxPoolSize > 0 : "poolSize must be greater than 0!";
		queue = new ArrayBlockingQueue<SftpSession>(maxPoolSize, true); // size, faireness to avoid starvation
		assert sftpSessionFactory != null : "sftpSessionFactory must not be null!";
	}

	public SftpSession getSession() throws Exception {
		SftpSession session = this.queue.poll();

		if (null == session) {
			session = this.sftpSessionFactory.getObject();

			if (queue.size() < maxPoolSize) {
				queue.add(session);
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
		} else {
			dispose(session);
		}
	}

	private void dispose(SftpSession s) {
		if (s == null) {
			return;
		}

		if (queue.contains(s)) //this should never happen, but if it does ...
		{
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
