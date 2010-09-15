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

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class TransactionalCallbackDecorator implements PollerCallbackDecorator, BeanFactoryAware {
	private BeanFactory beanFactory;
	
	private Properties transactionalProperties;

	public Properties getTransactionalProperties() {
		return transactionalProperties;
	}

	public void setTransactionalProperties(Properties transactionalProperties) {
		this.transactionalProperties = transactionalProperties;
	}

	public Object decorate(Object pollingCallback){
		TransactionProxyFactoryBean txFactoryBean = new TransactionProxyFactoryBean();
		txFactoryBean.setBeanFactory(beanFactory);
		PlatformTransactionManager txManager = (PlatformTransactionManager) this.beanFactory.getBean(transactionalProperties.getProperty("transactionManager"));
		txFactoryBean.setTransactionManager(txManager);
		DefaultTransactionAttribute txDefinition = new DefaultTransactionAttribute();
		txDefinition.setPropagationBehaviorName(transactionalProperties.getProperty("PROPAGATION"));
		txDefinition.setIsolationLevelName(transactionalProperties.getProperty("ISOLATION"));
		txDefinition.setTimeout(Integer.valueOf(transactionalProperties.getProperty("timeout")));
		txDefinition.setReadOnly(transactionalProperties.getProperty("readOnly").equalsIgnoreCase("true"));
		MatchAlwaysTransactionAttributeSource attributeSource = new MatchAlwaysTransactionAttributeSource();
		attributeSource.setTransactionAttribute(txDefinition);
		txFactoryBean.setTransactionAttributeSource(attributeSource);
		txFactoryBean.setTarget(pollingCallback);
		txFactoryBean.afterPropertiesSet();
		return txFactoryBean.getObject();
	}
	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
