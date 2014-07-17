/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.IntegrationResourceHolderSynchronization;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
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

	private volatile TransactionSynchronizationFactory transactionSynchronizationFactory;

	public AbstractPollingEndpoint() {
		this.setPhase(Integer.MAX_VALUE / 2);
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

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setTransactionSynchronizationFactory(TransactionSynchronizationFactory transactionSynchronizationFactory) {
		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
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
			@Override
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

	private boolean doPoll() {
		IntegrationResourceHolder holder = this.bindResourceHolderIfNecessary(
				this.getResourceKey(), this.getResourceToBind());
		Message<?> message = this.receiveMessage();
		boolean result;
		if (message == null) {
			if (this.logger.isDebugEnabled()){
				this.logger.debug("Received no Message during the poll, returning 'false'");
			}
			result = false;
		}
		else {
			if (this.logger.isDebugEnabled()){
				this.logger.debug("Poll resulted in Message: " + message);
			}
			if (holder != null) {
				holder.setMessage(message);
			}
			this.handleMessage(message);
			result = true;
		}
		return result;
	}

	/**
	 * Obtain the next message (if one is available). MAY return null
	 * if no message is immediately available.
	 * @return The message or null.
	 */
	protected abstract Message<?> receiveMessage();

	/**
	 * Handle a message.
	 * @param message The message.
	 */
	protected abstract void handleMessage(Message<?> message);

	/**
	 * Return a resource (MessageSource etc) to bind when using transaction
	 * synchronization.
	 * @return The resource, or null if transaction synchronization is not required.
	 */
	protected Object getResourceToBind() {
		return null;
	}

	/**
	 * Return the key under which the resource will be made available as an
	 * attribute on the {@link IntegrationResourceHolder}. The default
	 * {@link ExpressionEvaluatingTransactionSynchronizationProcessor}
	 * makes this attribute available as a variable in SpEL expressions.
	 * @return The key, or null (default) if the resource shouldn't be
	 * made available as a attribute.
	 */
	protected String getResourceKey() {
		return null;
	}

	private IntegrationResourceHolder bindResourceHolderIfNecessary(String key, Object resource) {

		if (this.transactionSynchronizationFactory != null && resource != null) {
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionSynchronization synchronization = this.transactionSynchronizationFactory.create(resource);
				TransactionSynchronizationManager.registerSynchronization(synchronization);
				if (synchronization instanceof IntegrationResourceHolderSynchronization) {
					IntegrationResourceHolder holder =
							((IntegrationResourceHolderSynchronization) synchronization).getResourceHolder();
					if (key != null) {
						holder.addAttribute(key, resource);
					}
					return holder;
				}
			}
		}
		return null;
	}

	/**
	 * Default Poller implementation
	 */
	private class Poller implements Runnable {

		private final Callable<Boolean> pollingTask;


		public Poller(Callable<Boolean> pollingTask) {
			this.pollingTask = pollingTask;
		}

		@Override
		public void run() {
			taskExecutor.execute(new Runnable() {
				@Override
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
								throw new MessageHandlingException(new ErrorMessage(e), e);
							}
						}
					}
				}
			});
		}

	}

}
