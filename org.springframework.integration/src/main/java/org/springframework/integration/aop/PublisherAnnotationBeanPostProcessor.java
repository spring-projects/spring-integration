/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.core.MessageChannel;

/**
 * Will post process beans that contain @{@link Publisher} annotation.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class PublisherAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

	private BeanFactory beanFactory;
	private MessageChannel defaultChannel;
	private PublisherAnnotationAdvisor advisor;
	/**
	 * 
	 */
	public PublisherAnnotationBeanPostProcessor(){}
	/**
	 * 
	 * @param defaultChannel
	 */
	public PublisherAnnotationBeanPostProcessor(MessageChannel defaultChannel){
		this.defaultChannel = defaultChannel;
	}
	/**
	 * 
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (this.containsPublisherAnnotations(bean)){
			ProxyFactory pf = new ProxyFactory(bean);
			pf.addAdvisor(advisor);
			bean = pf.getProxy();
		}
		return bean;
	}
	/**
	 * 
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}
	/**
	 * 
	 * @return
	 */
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}
	/**
	 * 
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
	/**
	 * 
	 */
	public void afterPropertiesSet(){
		advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(beanFactory);
		advisor.setDefaultChannel(defaultChannel);
	}
	/**
	 * 
	 * @param bean
	 * @return
	 */
	private boolean containsPublisherAnnotations(Object bean){
		Method[] methods = bean.getClass().getMethods();
		for (Method method : methods) {
			Annotation publisher = AnnotationUtils.findAnnotation(method, Publisher.class);
			if (publisher != null){
				return true;
			}
		}
		return false;
	}
}
