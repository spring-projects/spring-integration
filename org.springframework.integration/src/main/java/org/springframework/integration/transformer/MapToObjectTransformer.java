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
package org.springframework.integration.transformer;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MapToObjectTransformer extends AbstractPayloadTransformer<Map<?,?>, Object> implements BeanFactoryAware{
	private Object target;
	private String targetBeanName;
	private ConfigurableBeanFactory beanFactory;
	/**
	 * 
	 * @param targetClass
	 */
	public MapToObjectTransformer(Class<?> targetClass){
		try {
			this.target = BeanUtils.instantiate(targetClass);
		} catch (Exception e) {
			throw new MessageTransformationException("Can not create instance of " + targetClass, e);
		}	
	}
	/**
	 * 
	 * @param beanName
	 */
	public MapToObjectTransformer(String beanName){
		this.targetBeanName = beanName;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.transformer.AbstractPayloadTransformer#transformPayload(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	protected Object transformPayload(Map<?,?> payload) throws Exception {
		if (StringUtils.hasText(targetBeanName)){
			Assert.isTrue(!beanFactory.isSingleton(targetBeanName), "bean " + targetBeanName + " must be 'prototype'");
			target = beanFactory.getBean(targetBeanName);
		}
		DataBinder binder = new DataBinder(target);	
		binder.setConversionService(beanFactory.getConversionService());
		MutablePropertyValues pv = new MutablePropertyValues((Map)payload);
		binder.bind(pv);
		return target;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}
}
