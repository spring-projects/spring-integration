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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

import com.jcraft.jsch.Channel;

/**
 * This approach - of having a SessionPool ({@link SftpSessionPool}) that has an
 * implementation of a queued SessionPool ({@link QueuedSftpSessionPool}) - was
 * taken almost directly from the Spring Integration FTP adapter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class QueuedSftpSessionPool implements SftpSessionPool, SmartLifecycle {

	private static Logger logger = Logger.getLogger(QueuedSftpSessionPool.class.getName());

	public static final int DEFAULT_POOL_SIZE = 10;


	private volatile Queue<SftpSession> queue;

	private final SimpleSftpSessionFactory sftpSessionFactory;

	private final int maxPoolSize;
	
	private volatile boolean running;

	private volatile boolean autoStartup;

	private volatile int phase = 0;

	private final ReentrantLock lock = new ReentrantLock();


	public QueuedSftpSessionPool(SimpleSftpSessionFactory factory) {
		this(DEFAULT_POOL_SIZE, factory);
	}

	public QueuedSftpSessionPool(int maxPoolSize, SimpleSftpSessionFactory sessionFactory) {
		this.sftpSessionFactory = sessionFactory;
		this.maxPoolSize = maxPoolSize;
	}


	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public SftpSession getSession() {
		Assert.notNull(this.queue, "SftpSession is unavailable since the pool component is not started");
		this.lock.lock();
		try {
			SftpSession session = this.queue.poll();
			if (null == session) {
				session = this.sftpSessionFactory.getSession();
			}
			return session;
		} 
		finally {
			this.lock.unlock();
		}
		
	}

	public void release(SftpSession sftpSession) {
		if (this.running) {
			this.lock.lock();
			try {
				if (queue.size() < maxPoolSize && sftpSession != null) {
					queue.add(sftpSession); 
				}
				else {
					this.destroySftpSession(sftpSession);
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		else {
			this.destroySftpSession(sftpSession);
		}
	}


	private void destroySftpSession(SftpSession sftpSession) {
		try {
			if (sftpSession != null) {
				Channel channel = sftpSession.getChannel();
				if (channel.isConnected()) {
					channel.disconnect();
				}
				sftpSession.disconnect();
			}	
		}
		catch (Throwable e) {
			// log and ignore
			logger.warning("Exception was thrown while destroying SftpSession. " + e);
		}
	}


	// SmartLifeycle implementation

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public int getPhase() {
		return this.phase;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		Assert.isTrue(this.maxPoolSize > 0, "poolSize must be greater than 0");
		this.lock.lock();
		try {
			this.queue = new ArrayBlockingQueue<SftpSession>(this.maxPoolSize, true);
			this.running = true;
		}
		finally {
			this.lock.unlock();
		}
	}

	public void stop() {
		if (this.queue != null) {
			for (SftpSession sftpSession : this.queue) {
				this.destroySftpSession(sftpSession);
			}
		}
	}

	public void stop(Runnable callback) {
		this.lock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.running = false;
			this.lock.unlock();
		}
	}

}
