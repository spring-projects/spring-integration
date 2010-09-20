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
package org.springframework.integration.scheduling;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.config.Poller;
import org.springframework.integration.util.AsyncInvokerAdvice;
import org.springframework.integration.util.ObjectDecorator;
import org.springframework.util.CollectionUtils;
/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PollerFactory implements BeanClassLoaderAware, BeanFactoryAware {
	private final Log logger = LogFactory.getLog(this.getClass());
	private volatile ClassLoader beanClassLoader;
	private volatile BeanFactory beanFactory;
	
	private volatile PollerMetadata pollerMetadata;
	/**
	 * 
	 */
	public PollerFactory(){}
	/**
	 * 
	 * @param pollerMetadata
	 */
	public PollerFactory(PollerMetadata pollerMetadata){
		this.pollerMetadata = pollerMetadata;
	}
	/**
	 * 
	 * @param pollingTask
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Runnable createPoller(Callable<Boolean> pollingTask) throws Exception {
		if (this.taskDecorationRequired()){
			ProxyFactory proxyFactory = new ProxyFactory(pollingTask);
			pollingTask = (Callable<Boolean>) proxyFactory.getProxy(this.beanClassLoader);
			ObjectDecorator transactionDecorator = this.pollerMetadata.getTransactionDecorator();
			// take care of TransactionINterceptor first
			if (transactionDecorator != null){
				pollingTask = (Callable<Boolean>) transactionDecorator.decorate(pollingTask);
				logger.info("Polling task has been decorated with TransactionInterceptor to handle transactions");
			}
			// ... then add more Advises if provided
			List<Advice> advices = this.pollerMetadata.getAdviceChain();
			if (advices != null){
				for (Advice advice : advices) {
					((Advised)pollingTask).addAdvice(advice);
					logger.info("Polling task has been decorated with " + advice.getClass().getSimpleName());
				}
			}
		} 
		Runnable poller = new Poller(pollingTask);
		if (pollerMetadata != null){
			((Poller)poller).setMaxMessagesPerPoll(this.pollerMetadata.getMaxMessagesPerPoll());
		}
		// Decorate Poller with AsyncInvokerAdvice
		Executor taskExecutor = this.pollerMetadata.getTaskExecutor();
		if (taskExecutor != null){
			ProxyFactory proxyFactory = new ProxyFactory(poller);
			
			AsyncInvokerAdvice asyncInvokerAdvice = new AsyncInvokerAdvice(taskExecutor);
			asyncInvokerAdvice.setBeanFactory(this.beanFactory);
			asyncInvokerAdvice.afterPropertiesSet();
			proxyFactory.addAdvice(asyncInvokerAdvice);
			poller = (Runnable) proxyFactory.getProxy(this.beanClassLoader);
			logger.info("Poller has been decorated with AsyncInvokerAdvice for async polling");
		}
		return poller;
	}
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}
	private boolean taskDecorationRequired(){
		return pollerMetadata != null && 
				(	this.pollerMetadata.getTransactionDecorator() != null || 
					!CollectionUtils.isEmpty(this.pollerMetadata.getAdviceChain())	);
	}
}
