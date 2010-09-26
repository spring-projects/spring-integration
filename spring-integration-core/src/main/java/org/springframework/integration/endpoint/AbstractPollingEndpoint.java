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
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;
/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements BeanClassLoaderAware{
	
	private volatile TaskExecutor taskExecutor = new SyncTaskExecutor();
	
	private ErrorHandler errorHandler;

	private volatile PollerMetadata pollerMetadata;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
	
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
	
	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			
			Assert.notNull(this.pollerMetadata.getTrigger(), "trigger is required");
			Assert.notNull(this.getBeanFactory(), "BeanFactory must be provided");
			
			TaskExecutor executor = pollerMetadata.getTaskExecutor();
			if (executor != null){
				taskExecutor = executor;
			}
			
			if (taskExecutor != null){
				if (!(taskExecutor instanceof ErrorHandlingTaskExecutor)) {				
					if (errorHandler == null) {
						errorHandler = new MessagePublishingErrorHandler(
								new BeanFactoryChannelResolver(getBeanFactory()));
					}
					taskExecutor = new ErrorHandlingTaskExecutor(taskExecutor, errorHandler);
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
	private Runnable createPoller() throws Exception{
		
		Callable<Boolean> pollingTask = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return doPoll();
			}
		};
		
		Advisor transactionAdvice = this.pollerMetadata.getTransactionAdvisor();
		List<Advice> adviceChain = this.pollerMetadata.getAdviceChain();
		if (transactionAdvice != null || !CollectionUtils.isEmpty(adviceChain)){
			ProxyFactory proxyFactory = new ProxyFactory(pollingTask);
			
			// Add Transaction advice first
			if (transactionAdvice != null){
				proxyFactory.addAdvisor(transactionAdvice);
			}
			
			// . . .then add the rest of the advises
			if (!CollectionUtils.isEmpty(adviceChain)){
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
		this.runningTask = this.getTaskScheduler().schedule(this.poller, this.pollerMetadata.getTrigger());
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.runningTask != null) {
			this.runningTask.cancel(true);
		}
		this.runningTask = null;
		this.initialized = false;
	}
	
	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}
	
	public void setBeanClassLoader(ClassLoader classLoader){
		this.beanClassLoader = classLoader;
	}
	
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}
	
	protected abstract boolean doPoll();
	/**
	 * Default Poller implementation
	 */
	private class Poller implements Runnable {
		private final long maxMessagesPerPoll = pollerMetadata.getMaxMessagesPerPoll();
		private final Callable<Boolean> pollingTask;
		
		public Poller(Callable<Boolean> pollingTask){
			this.pollingTask = pollingTask;
		}

		public void run() {
			
			taskExecutor.execute(new Runnable() {
				
				public void run() {
					int count = 0;
					while (maxMessagesPerPoll <= 0 || count < maxMessagesPerPoll) {
						try {
							boolean computed = pollingTask.call();
							if (!computed){
								break;
							}
							count++;
						} 
						catch (Exception e) {
							if (e instanceof RuntimeException) {
								throw (RuntimeException)e;
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
