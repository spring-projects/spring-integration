/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

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
 * @author Kris Jacyna
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractEndpoint extends IntegrationObjectSupport
		implements SmartLifecycle, DisposableBean {

	private boolean autoStartupSetExplicitly;

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean running;

	protected final ReentrantLock lifecycleLock = new ReentrantLock();

	protected final Condition lifecycleCondition = this.lifecycleLock.newCondition();

	private String role;

	private SmartLifecycleRoleController roleController;

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		this.autoStartupSetExplicitly = true;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Specify the role for the endpoint.
	 * Such endpoints can be started/stopped as a group.
	 * @param role the role for this endpoint.
	 * @since 5.0
	 * @see SmartLifecycle
	 * @see org.springframework.integration.support.SmartLifecycleRoleController
	 */
	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return this.role;
	}

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();

		if (!this.autoStartupSetExplicitly) {
			String[] endpointNamePatterns =
					getIntegrationProperty(IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP, String[].class);

			for (String pattern : endpointNamePatterns) {
				if (PatternMatchUtils.simpleMatch(pattern, getComponentName())) {
					this.autoStartup = false;
					break;
				}
			}
		}

		if (StringUtils.hasText(this.role)) {
			try {
				this.roleController = getBeanFactory()
						.getBean(IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER,
								SmartLifecycleRoleController.class);

				this.roleController.addLifecycleToRole(this.role, this);
			}
			catch (NoSuchBeanDefinitionException e) {
					this.logger.trace("No LifecycleRoleController in the context");
				}
		}
	}

	@Override
	public void destroy() throws Exception {
		if (this.roleController != null) {
			this.roleController.removeLifecycle(this);
		}
	}

	// SmartLifecycle implementation

	@Override
	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public final int getPhase() {
		return this.phase;
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
	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				doStart();
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
	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				doStop();
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

	@Override
	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				doStop(callback);
				this.running = false;
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
			else {
				callback.run();
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Stop the component and invoke callback.
	 * @param callback the Runnable to invoke.
	 */
	protected void doStop(Runnable callback) {
		doStop();
		callback.run();
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
