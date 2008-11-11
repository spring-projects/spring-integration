/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.util;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * A convenience base class for Lifecycle components that supports an
 * "auto-startup" mode property. Depending on the mode, the component can
 * be started either upon initialization, upon receiving the
 * {@link ContextRefreshedEvent}, or may require an explicit start invocation.
 * The timing of the startup is determined by the value of {@link #autoStartMode}.
 * The default value is {@link AutoStartMode#ON_INIT}. To require explicit startup,
 * set the mode to {@link AutoStartMode#NONE} using the
 * {@link #setAutoStartMode(AutoStartMode)} method.
 * 
 * @author Mark Fisher
 */
public abstract class LifecycleSupport implements Lifecycle, InitializingBean, ApplicationListener {

	public static enum AutoStartMode { ON_INIT, ON_CONTEXT_REFRESH, NONE }


	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile AutoStartMode autoStartMode = AutoStartMode.ON_INIT;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();


	public void setAutoStartMode(AutoStartMode autoStartMode) {
		this.autoStartMode = (autoStartMode != null) ? autoStartMode : AutoStartMode.NONE;
	}

	public final void afterPropertiesSet() {
		try {
			this.onInit();
			if (this.autoStartMode == AutoStartMode.ON_INIT) {
				this.start();
			}
		}
		catch (Exception e) {
			throw new BeanInitializationException("failed to initialize", e);
		}
	}

	public final void onApplicationEvent(ApplicationEvent event) {
		this.onEvent(event);
		if (event instanceof ContextRefreshedEvent && this.autoStartMode == AutoStartMode.ON_CONTEXT_REFRESH) {
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

	protected void onEvent(ApplicationEvent event) {
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
