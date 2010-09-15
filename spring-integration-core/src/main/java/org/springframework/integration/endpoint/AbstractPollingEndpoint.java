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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements InitializingBean, BeanClassLoaderAware {

	public static final int MAX_MESSAGES_UNBOUNDED = -1;


	private volatile Trigger trigger;

	protected volatile long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED; 

	private volatile Executor taskExecutor;

	private volatile ErrorHandler errorHandler;
	
	private PollerCallbackDecorator pollingDecorator;

	public void setPollingDecorator(PollerCallbackDecorator pollingDecorator) {
		this.pollingDecorator = pollingDecorator;
	}

	private final List<Advice> adviceChain = new CopyOnWriteArrayList<Advice>();

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	private volatile ScheduledFuture<?> runningTask;

	private volatile Runnable poller;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractPollingEndpoint() {
		this.setPhase(Integer.MAX_VALUE);
	}


	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	/**
	 * Set the maximum number of messages to receive for each poll.
	 * A non-positive value indicates that polling should repeat as long
	 * as non-null messages are being received and successfully sent.
	 * 
	 * <p>The default is unbounded.
	 * 
	 * @see #MAX_MESSAGES_UNBOUNDED
	 */
	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setErrorHandler(ErrorHandler errorHandler){
		this.errorHandler = errorHandler;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
	}
//
	public void setAdviceChain(List<Advice> adviceChain) {
		synchronized (this.adviceChain) {
			this.adviceChain.clear();
			if (adviceChain != null) {
				this.adviceChain.addAll(adviceChain);
			}
		}
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.trigger, "trigger is required");
			if (this.taskExecutor != null && !(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
				if (this.errorHandler == null) {
					this.errorHandler = new MessagePublishingErrorHandler(new BeanFactoryChannelResolver(getBeanFactory()));
				}
				this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, this.errorHandler);
			}
			this.poller = this.createPoller();
			this.initialized = true;
		}
	}

	private Runnable createPoller() {
		Runnable poller = new Poller();
		if (pollingDecorator != null){
			poller = (Runnable) pollingDecorator.decorate(poller);
		}
		if (poller instanceof Advised){
			Advised advised = (Advised) poller;
			for (Advice advice : adviceChain) {
				advised.addAdvice(advice);
			}
		} else {
			if (adviceChain.size() > 0){
				ProxyFactory proxyFactory = new ProxyFactory(poller);
				for (Advice advice : adviceChain) {
					proxyFactory.addAdvice(advice);
				}
				poller = (Runnable) proxyFactory.getProxy(this.classLoader);
			}
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


	private class Poller implements Runnable {

		public void run() {
			if (taskExecutor != null) {
				taskExecutor.execute(new Runnable() {
					public void run() {
						poll();
					}
				});
			}
			else {
				poll();
			}
		}

		private void poll() {
			int count = 0;
			while (maxMessagesPerPoll <= 0 || count < maxMessagesPerPoll) {
				if (!innerPoll()) {
					break;
				}
				count++;
			}
		}

		private boolean innerPoll() {
			return doPoll();
		}
	}
}
