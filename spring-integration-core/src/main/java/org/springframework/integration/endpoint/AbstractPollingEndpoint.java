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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.IntegrationResourceHolderSynchronization;
import org.springframework.integration.transaction.PassThroughTransactionSynchronizationFactory;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.interceptor.TransactionInterceptor;
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
 * @author Andreas Baer
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements BeanClassLoaderAware {

	private volatile Executor taskExecutor = new SyncTaskExecutor();

	private volatile ErrorHandler errorHandler;

	private volatile boolean errorHandlerIsDefault;

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

	public void setTransactionSynchronizationFactory(TransactionSynchronizationFactory
			transactionSynchronizationFactory) {
		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
	}

	/**
	 * Return the default error channel if the error handler is explicitly provided and
	 * it is a {@link MessagePublishingErrorHandler}.
	 * @return the channel or null.
	 * @since 4.3
	 */
	public MessageChannel getDefaultErrorChannel() {
		if (!this.errorHandlerIsDefault && this.errorHandler instanceof MessagePublishingErrorHandler) {
			return ((MessagePublishingErrorHandler) this.errorHandler).getDefaultErrorChannel();
		}
		else {
			return null;
		}
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Return true if this advice should be applied only to the {@link #receiveMessage()} operation
	 * rather than the whole poll.
	 * @param advice The advice.
	 * @return true to only advise the receive operation.
	 */
	protected boolean isReceiveOnlyAdvice(Advice advice) {
		return false;
	}

	/**
	 * Add the advice chain to the component that responds to {@link #receiveMessage()} calls.
	 * @param chain the advice chain {@code Collection}.
	 */
	protected void applyReceiveOnlyAdviceChain(Collection<Advice> chain) {
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.trigger, "Trigger is required");
			if (this.taskExecutor != null) {
				if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
					if (this.errorHandler == null) {
						Assert.notNull(this.getBeanFactory(), "BeanFactory is required");
						this.errorHandler = new MessagePublishingErrorHandler(
								new BeanFactoryChannelResolver(getBeanFactory()));
						this.errorHandlerIsDefault = true;
					}
					this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, this.errorHandler);
				}
			}
			if (this.transactionSynchronizationFactory == null && this.adviceChain != null) {
				if (this.adviceChain.stream().anyMatch(TransactionInterceptor.class::isInstance)) {
					this.transactionSynchronizationFactory = new PassThroughTransactionSynchronizationFactory();
				}
			}
			this.initialized = true;
		}
		try {
			super.onInit();
		}
		catch (Exception e) {
			throw new BeanInitializationException("Cannot initialize: " + this, e);
		}
	}

	@SuppressWarnings("unchecked")
	private Runnable createPoller() throws Exception {
		List<Advice> receiveOnlyAdviceChain = null;
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			receiveOnlyAdviceChain = this.adviceChain.stream()
					.filter(this::isReceiveOnlyAdvice)
					.collect(Collectors.toList());
		}

		Callable<Boolean> pollingTask = this::doPoll;

		List<Advice> adviceChain = this.adviceChain;
		if (!CollectionUtils.isEmpty(adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(pollingTask);
			if (!CollectionUtils.isEmpty(adviceChain)) {
				adviceChain.stream()
						.filter(advice -> !isReceiveOnlyAdvice(advice))
						.forEach(proxyFactory::addAdvice);
			}
			pollingTask = (Callable<Boolean>) proxyFactory.getProxy(this.beanClassLoader);
		}
		if (!CollectionUtils.isEmpty(receiveOnlyAdviceChain)) {
			applyReceiveOnlyAdviceChain(receiveOnlyAdviceChain);
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
		try {
			this.poller = createPoller();
		}
		catch (Exception e) {
			this.initialized = false;
			throw new MessagingException("Failed to create Poller", e);
		}
		this.runningTask = this.getTaskScheduler().schedule(this.poller, this.trigger);
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.runningTask != null) {
			this.runningTask.cancel(true);
		}
		this.runningTask = null;
	}

	private boolean doPoll() {
		IntegrationResourceHolder holder = this.bindResourceHolderIfNecessary(
				this.getResourceKey(), this.getResourceToBind());
		Message<?> message = null;
		try {
			message = this.receiveMessage();
		}
		catch (Exception e) {
			if (Thread.interrupted()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Poll interrupted - during stop()? : " + e.getMessage());
				}
				return false;
			}
			else {
				throw (RuntimeException) e;
			}
		}
		boolean result;
		if (message == null) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Received no Message during the poll, returning 'false'");
			}
			result = false;
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Poll resulted in Message: " + message);
			}
			if (holder != null) {
				holder.setMessage(message);
			}
			try {
				this.handleMessage(message);
			}
			catch (Exception e) {
				if (e instanceof MessagingException) {
					throw new MessagingExceptionWrapper(message, (MessagingException) e);
				}
				else {
					throw new MessagingException(message, e);
				}
			}
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
		if (this.transactionSynchronizationFactory != null && resource != null &&
				TransactionSynchronizationManager.isActualTransactionActive()) {

			TransactionSynchronization synchronization = this.transactionSynchronizationFactory.create(resource);
			if (synchronization != null) {
				TransactionSynchronizationManager.registerSynchronization(synchronization);

				if (synchronization instanceof IntegrationResourceHolderSynchronization) {
					IntegrationResourceHolderSynchronization integrationSynchronization =
							((IntegrationResourceHolderSynchronization) synchronization);
					integrationSynchronization.setShouldUnbindAtCompletion(false);

					if (!TransactionSynchronizationManager.hasResource(resource)) {
						TransactionSynchronizationManager.bindResource(resource,
								integrationSynchronization.getResourceHolder());
					}
				}
			}

			Object resourceHolder = TransactionSynchronizationManager.getResource(resource);
			if (resourceHolder instanceof IntegrationResourceHolder) {
				IntegrationResourceHolder integrationResourceHolder = (IntegrationResourceHolder) resourceHolder;
				if (key != null) {
					integrationResourceHolder.addAttribute(key, resource);
				}
				return integrationResourceHolder;
			}
		}

		return null;
	}

	/**
	 * Default Poller implementation
	 */
	private final class Poller implements Runnable {

		private final Callable<Boolean> pollingTask;

		Poller(Callable<Boolean> pollingTask) {
			this.pollingTask = pollingTask;
		}

		@Override
		public void run() {
			AbstractPollingEndpoint.this.taskExecutor.execute(() -> {
				int count = 0;
				while (AbstractPollingEndpoint.this.initialized
						&& (AbstractPollingEndpoint.this.maxMessagesPerPoll <= 0
						|| count < AbstractPollingEndpoint.this.maxMessagesPerPoll)) {
					try {
						if (!Poller.this.pollingTask.call()) {
							break;
						}
						count++;
					}
					catch (Exception e) {
						if (e instanceof MessagingException) {
							throw (MessagingException) e;
						}
						else {
							Message<?> failedMessage = null;
							if (AbstractPollingEndpoint.this.transactionSynchronizationFactory != null) {
								Object resource = TransactionSynchronizationManager.getResource(getResourceToBind());
								if (resource instanceof IntegrationResourceHolder) {
									failedMessage = ((IntegrationResourceHolder) resource).getMessage();
								}
							}
							throw new MessagingException(failedMessage, e);
						}
					}
					finally {
						if (AbstractPollingEndpoint.this.transactionSynchronizationFactory != null) {
							Object resource = getResourceToBind();
							if (TransactionSynchronizationManager.hasResource(resource)) {
								TransactionSynchronizationManager.unbindResource(resource);
							}
						}
					}
				}
			});
		}

	}

}
