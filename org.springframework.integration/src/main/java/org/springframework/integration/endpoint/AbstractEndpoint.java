/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.scheduling.TaskScheduler;

/**
 * The base class for Message Endpoint implementations.
 * 
 * <p>This class implements Lifecycle and provides an {@link #autoStartup}
 * property. If <code>true</code>, the endpoint will start automatically upon
 * initialization. Otherwise, it will require an explicit invocation of its
 * {@link #start()} method. The default value is <code>true</code>.
 * To require explicit startup, provide a value of <code>false</code>
 * to the {@link #setAutoStartup(boolean)} method.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint extends IntegrationObjectSupport
		implements ApplicationListener, Lifecycle, InitializingBean {

	public static enum StartupMode {
		MANUAL,
		ON_INITIALIZATION,
		ON_CONTEXT_REFRESH;
	}


	private volatile StartupMode startupMode = StartupMode.MANUAL;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();


	public void setStartupMode(StartupMode startupMode) {
		this.startupMode = (startupMode != null ? startupMode : StartupMode.MANUAL);
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	public final void afterPropertiesSet() {
		try {
			this.onInit();
			if (this.startupMode == StartupMode.ON_INITIALIZATION) {
				this.start();
			}
		}
		catch (Exception e) {
			throw new BeanInitializationException("failed to initialize", e);
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent
				&& this.startupMode == StartupMode.ON_CONTEXT_REFRESH) {
			this.start();
		}
	}

	// Lifecycle implementation

	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.doStart();
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

	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	protected void onInit() throws Exception {
	}

	/**
	 * Subclasses must implement this method with the start behavior.
	 * This method will be invoked while holding the {@link #lifecycleLock}.
	 */
	protected abstract void doStart();

	/**
	 * Subclasses must implement this method with the stop behavior.
	 * This method will be invoked while holding the {@link #lifecycleLock}.
	 */
	protected abstract void doStop();

}
