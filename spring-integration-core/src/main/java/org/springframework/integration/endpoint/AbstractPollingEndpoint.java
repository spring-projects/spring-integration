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
package org.springframework.integration.endpoint;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingException;
import org.springframework.integration.config.Poller;
import org.springframework.integration.scheduling.PollerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;
/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements InitializingBean{

	private volatile Trigger trigger;

	private  PollerFactory pollerFactory;

	private volatile ScheduledFuture<?> runningTask;

	private volatile Runnable poller;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();
	/**
	 * 
	 */
	public AbstractPollingEndpoint() {
		this.setPhase(Integer.MAX_VALUE);
	}
	/**
	 * @param trigger
	 */
	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}
	/**
	 * @param pollerFactory
	 */
	public void setPollerFactory(PollerFactory pollerFactory) {
		this.pollerFactory = pollerFactory;
	}
	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.trigger, "trigger is required");
			try {
				this.poller = this.createPoller();
				this.initialized = true;
			} catch (Exception e) {
				throw new MessagingException("Problems creating a poller", e);
			}
		}
	}

	private Runnable createPoller() throws Exception{
		Callable<Boolean> pollingTask = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return doPoll();
			}
		};
		if (pollerFactory == null){
			poller = new Poller(pollingTask);
		} else {
			poller = pollerFactory.createPoller(pollingTask);
		}
		return poller;
	}

	// LifecycleSupport implementation

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		if (!this.initialized) {
			this.onInit();
		}
		Assert.state(this.getTaskScheduler() != null,
				"unable to start polling, no taskScheduler available");
		this.runningTask = this.getTaskScheduler().schedule(this.poller, this.trigger);
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.runningTask != null) {
			this.runningTask.cancel(true);
		}
		this.runningTask = null;
	}

	protected abstract boolean doPoll();
}
