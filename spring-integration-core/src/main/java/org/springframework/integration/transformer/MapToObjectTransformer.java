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

package org.springframework.integration.transformer;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * Will transform Map to an instance of Object. There are two ways to specify the type of the transformed Object.
 * You can use one of two constructors. The constructor that takes the Class&lt;?&gt; as an argument will construct the Object of 
 * that type. There is another constructor that takes a 'beanName' as an argument and will populate this bean with transformed data.
 * Such bean must be of 'prototype' scope otherwise {@link MessageTransformationException} will be thrown.
 * This transformer is integrated with the {@link ConversionService} allowing values in the Map to be converted 
 * to types that represent the properties of the Object.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MapToObjectTransformer extends AbstractPayloadTransformer<Map<?,?>, Object> implements BeanFactoryAware{

	private final Class<?> targetClass;

	private final String targetBeanName;

	private volatile ConfigurableBeanFactory beanFactory;


	/**
	 * @param targetClass
	 */
	public MapToObjectTransformer(Class<?> targetClass) {
		Assert.notNull(targetClass, "targetClass must not be null");
		this.targetClass = targetClass;
		this.targetBeanName = null;
	}

	/**
	 * @param beanName
	 */
	public MapToObjectTransformer(String beanName) {
		Assert.hasText(beanName, "beanName must not be empty");
		this.targetBeanName = beanName;
		this.targetClass = null;
	}


	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.transformer.AbstractPayloadTransformer#transformPayload(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	protected Object transformPayload(Map<?,?> payload) throws Exception {
		Object target = (this.targetClass != null)
				? BeanUtils.instantiate(this.targetClass)
				: this.beanFactory.getBean(this.targetBeanName);
		DataBinder binder = new DataBinder(target);	
		binder.setConversionService(this.beanFactory.getConversionService());
		binder.bind(new MutablePropertyValues((Map) payload));
		return target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isTrue(beanFactory instanceof ConfigurableListableBeanFactory,
				"A ConfigurableListableBeanFactory is required.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		if (StringUtils.hasText(this.targetBeanName)) {
			Assert.isTrue(this.beanFactory.isPrototype(this.targetBeanName),
					"target bean [" + targetBeanName + "] must have 'prototype' scope");
		}
	}

}
