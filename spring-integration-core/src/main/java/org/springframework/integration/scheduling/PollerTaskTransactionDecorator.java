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

import java.util.Properties;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.endpoint.Poller;
import org.springframework.integration.util.ObjectDecorator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * A simple implementation of {@link ObjectDecorator} which will add 
 * {@link TransactionInterceptor} advice to any instance of {@link Advised}.
 * Currently used to decorate {@link Poller}'s <code>pollingTask</code>.  
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PollerTaskTransactionDecorator implements ObjectDecorator, BeanFactoryAware {
	private BeanFactory beanFactory;
	private Properties transactionalProperties;

	/* (non-Javadoc)
	 * @see org.springframework.integration.util.ObjectDecorator#decorate(java.lang.Object)
	 */
	public Object decorate(Object advisedPollingTask) {
		Assert.isInstanceOf(Advised.class, advisedPollingTask, "'pollingTask' must be an instance of Advised");
		PlatformTransactionManager txManager = (PlatformTransactionManager) this.beanFactory.getBean(transactionalProperties.getProperty("transactionManager"));
		DefaultTransactionAttribute txDefinition = new DefaultTransactionAttribute();
		txDefinition.setPropagationBehaviorName(transactionalProperties.getProperty("PROPAGATION"));
		txDefinition.setIsolationLevelName(transactionalProperties.getProperty("ISOLATION"));
		txDefinition.setTimeout(Integer.valueOf(transactionalProperties.getProperty("timeout")));
		txDefinition.setReadOnly(transactionalProperties.getProperty("readOnly").equalsIgnoreCase("true"));
		MatchAlwaysTransactionAttributeSource attributeSource = new MatchAlwaysTransactionAttributeSource();
		attributeSource.setTransactionAttribute(txDefinition);
		
		TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
		transactionInterceptor.setTransactionManager(txManager);
		transactionInterceptor.setTransactionAttributeSource(attributeSource);
		transactionInterceptor.afterPropertiesSet();
		((Advised)advisedPollingTask).addAdvisor(new TransactionAttributeSourceAdvisor(transactionInterceptor));
		
		return advisedPollingTask;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	
	public Properties getTransactionalProperties() {
		return transactionalProperties;
	}

	public void setTransactionalProperties(Properties transactionalProperties) {
		this.transactionalProperties = transactionalProperties;
	}
}
