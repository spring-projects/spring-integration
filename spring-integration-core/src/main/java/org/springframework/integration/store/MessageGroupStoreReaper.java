/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.store;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * Convenient configurable component to allow explicit timed expiry of {@link MessageGroup} instances in a
 * {@link MessageGroupStore}. This component provides a no-args {@link #run()} method that is useful for remote or timed
 * execution and a {@link #destroy()} method that can optionally be called on shutdown.
 *
 * @author Dave Syer
 * @author Dave Turanski
 * @author Artem Bilan
 */
public class MessageGroupStoreReaper implements Runnable, DisposableBean, InitializingBean, SmartLifecycle {

	private static Log logger = LogFactory.getLog(MessageGroupStoreReaper.class);

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private MessageGroupStore messageGroupStore;

	private boolean expireOnDestroy = false;

	private long timeout = -1;

	private int phase = 0;

	private boolean autoStartup = true;

	private volatile boolean running;

	public MessageGroupStoreReaper(MessageGroupStore messageGroupStore) {
		this.messageGroupStore = messageGroupStore;
	}

	public MessageGroupStoreReaper() {
	}

	/**
	 * Flag to indicate that the stores should be expired when this component is destroyed (i.e. usually when its
	 * enclosing {@link org.springframework.context.ApplicationContext} is closed).
	 * @param expireOnDestroy the flag value to set
	 */
	public void setExpireOnDestroy(boolean expireOnDestroy) {
		this.expireOnDestroy = expireOnDestroy;
	}

	/**
	 * Timeout in milliseconds (default -1). If negative then no groups ever time out. If greater than zero then all
	 * groups older than that value are expired when this component is {@link #run()}.
	 * @param timeout the timeout to set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * A message group store to expire according the other configurations.
	 * @param messageGroupStore the {@link MessageGroupStore} to set
	 */
	public void setMessageGroupStore(MessageGroupStore messageGroupStore) {
		this.messageGroupStore = messageGroupStore;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(this.messageGroupStore != null, "A MessageGroupStore must be provided");
	}

	@Override
	public void destroy() {
		if (this.expireOnDestroy) {
			if (this.isRunning()) {
				logger.info("Expiring all messages from message group store: " + this.messageGroupStore);
				this.messageGroupStore.expireMessageGroups(0);
			}
			else {
				logger.debug("'expireOnDestroy' is set to 'true' but the reaper is not currently running");
			}
		}
	}

	/**
	 * Expire all message groups older than the {@link #setTimeout(long) timeout} provided. Normally this method would
	 * be executed by a scheduled task.
	 */
	@Override
	public void run() {
		if (this.timeout >= 0 && this.isRunning()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Expiring all messages older than timeout=" + this.timeout + " from message group store: "
						+ this.messageGroupStore);
			}
			this.messageGroupStore.expireMessageGroups(this.timeout);
		}
	}

	@Override
	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.running = true;
				if (logger.isInfoEnabled()) {
					logger.info("started " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.destroy();
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
			this.running = false;
		}
		catch (Exception e) {
			logger.error("failed to stop bean", e);
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

}
