/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.metadata;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * {@link Runnable} component that deletes entries from a {@link MetadataStore}.
 * By default, it expects the value of each entry to be a timestamp, as is the default configuration when using the
 * {@link org.springframework.integration.handler.advice.IdempotentReceiverInterceptor} and the
 * {@link org.springframework.integration.selector.MetadataStoreSelector}. You can inject a
 * {@link Predicate} to implement an expiration condition.
 *
 * @author David Turanski
 **/

public class MetadataStoreReaper implements Runnable, SmartLifecycle {

	private final static Log logger = LogFactory.getLog(MetadataStoreReaper.class);

	private final MetadataStore metadataStore;

	private final Predicate<String> isExpired;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private volatile int phase = 0;

	private volatile boolean autoStartup = true;

	/**
	 * Remove entries from a {@link MetadataStore} based on a {@link Predicate} used to determine the expiry condition.
	 *
	 * @param metadataStore the MetadataStore.
	 * @param isExpired     the Predicate tested against the entry's value.
	 */
	public MetadataStoreReaper(MetadataStore metadataStore, Predicate<String> isExpired) {
		Assert.notNull(metadataStore, "'metadataStore' cannot be null");
		Assert.notNull(isExpired, "'isExpired predicate' cannot be null");
		this.metadataStore = metadataStore;
		this.isExpired = isExpired;
	}

	@Override
	public void run() {
		this.metadataStore.keySet().stream().forEach(k -> {
			if (this.isExpired.test(this.metadataStore.get(k))) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("removing entry with key %s", k));
				}
				this.metadataStore.remove(k);
			}
		});
	}

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

	public void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
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

	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

}
