/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements BeanClassLoaderAware {

	private volatile Executor taskExecutor = new SyncTaskExecutor();

	private volatile ErrorHandler errorHandler;

	private volatile Trigger trigger = new PeriodicTrigger(10);

	private volatile List<Advice> adviceChain;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile ScheduledFuture<?> runningTask;

	private volatile Runnable poller;

	private volatile boolean initialized;

	private volatile long maxMessagesPerPoll = -1;

	private final Object initializationMonitor = new Object();

	public AbstractPollingEndpoint() {
		this.setPhase(Integer.MAX_VALUE);
	}

	/**
	 * @deprecated  As of release 2.0.2, use individual setters
	 */
	@Deprecated
	public void setPollerMetadata(PollerMetadata pollerMetadata){
		Assert.notNull(pollerMetadata, "'pollerMetadata' must not be null.");
		this.setAdviceChain(pollerMetadata.getAdviceChain());
		this.setMaxMessagesPerPoll(pollerMetadata.getMaxMessagesPerPoll());
		this.setTaskExecutor(pollerMetadata.getTaskExecutor());
		this.setTrigger(pollerMetadata.getTrigger());
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = (taskExecutor != null ? taskExecutor : new SyncTaskExecutor());
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = (trigger != null ? trigger : new PeriodicTrigger(10));
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	public void setMaxMessagesPerPoll(long maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.trigger, "Trigger is required");
			Executor providedExecutor = this.taskExecutor;
			if (providedExecutor != null) {
				this.taskExecutor = providedExecutor;
			}
			if (this.taskExecutor != null) {
				if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
					if (this.errorHandler == null) {
						Assert.notNull(this.getBeanFactory(), "BeanFactory is required");
						this.errorHandler = new MessagePublishingErrorHandler(
								new BeanFactoryChannelResolver(getBeanFactory()));
					}
					this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, this.errorHandler);
				}
			}
			try {
				this.poller = this.createPoller();
				this.initialized = true;
			}
			catch (Exception e) {
				throw new MessagingException("Failed to create Poller", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Runnable createPoller() throws Exception {

		Callable<Boolean> pollingTask = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return doPoll();
			}
		};

		List<Advice> adviceChain = this.adviceChain;
		if (!CollectionUtils.isEmpty(adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(pollingTask);
			if (!CollectionUtils.isEmpty(adviceChain)) {
				for (Advice advice : adviceChain) {
					proxyFactory.addAdvice(advice);
				}
			}
			pollingTask = (Callable<Boolean>) proxyFactory.getProxy(this.beanClassLoader);
		}
		return new Poller(pollingTask);
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
		this.initialized = false;
	}

	/**
	 * @deprecated Starting with Spring Integration 3.0, subclasses must not
	 * implement this method. The methods in {@link AbstractTransactionSynchronizingPollingEndpoint}
	 * will be pulled up here and subclasses must implement doReceive() and handleMessage(Message<?> message)
	 * instead. Consider refactoring now to subclass {@link AbstractTransactionSynchronizingPollingEndpoint}
	 * to make 3.0 migration easier.
	 * @return true if a message was processed.
	 */
	@Deprecated
	protected abstract boolean doPoll();

	/**
	 * Default Poller implementation
	 */
	private class Poller implements Runnable {

		private final Callable<Boolean> pollingTask;


		public Poller(Callable<Boolean> pollingTask) {
			this.pollingTask = pollingTask;
		}

		public void run() {
			taskExecutor.execute(new Runnable() {
				public void run() {
					int count = 0;
					while (initialized && (maxMessagesPerPoll <= 0 || count < maxMessagesPerPoll)) {
						try {
							if (!pollingTask.call()) {
								break;
							}
							count++;
						}
						catch (Exception e) {
							if (e instanceof RuntimeException) {
								throw (RuntimeException) e;
							}
							else {
								throw new MessageHandlingException(new ErrorMessage(e));
							}
						}
					}
				}
			});
		}
	}

}
