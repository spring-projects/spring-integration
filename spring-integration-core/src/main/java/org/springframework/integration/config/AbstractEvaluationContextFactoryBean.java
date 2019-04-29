/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.util.Assert;

/**
 * Abstract class for integration evaluation context factory beans.
 *
 * @author Gary Russell
 *
 * @since 4.3.15
 *
 */
public abstract class AbstractEvaluationContextFactoryBean implements ApplicationContextAware, InitializingBean {

	private Map<String, PropertyAccessor> propertyAccessors = new LinkedHashMap<String, PropertyAccessor>();

	private Map<String, Method> functions = new LinkedHashMap<String, Method>();

	private TypeConverter typeConverter = new StandardTypeConverter();

	private ApplicationContext applicationContext;

	private boolean initialized;

	protected TypeConverter getTypeConverter() {
		return this.typeConverter;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setPropertyAccessors(Map<String, PropertyAccessor> accessors) {
		Assert.isTrue(!this.initialized, "'propertyAccessors' can't be changed after initialization.");
		Assert.notNull(accessors, "'accessors' must not be null.");
		Assert.noNullElements(accessors.values().toArray(), "'accessors' cannot have null values.");
		this.propertyAccessors = new LinkedHashMap<String, PropertyAccessor>(accessors);
	}

	public Map<String, PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	public void setFunctions(Map<String, Method> functionsArg) {
		Assert.isTrue(!this.initialized, "'functions' can't be changed after initialization.");
		Assert.notNull(functionsArg, "'functions' must not be null.");
		Assert.noNullElements(functionsArg.values().toArray(), "'functions' cannot have null values.");
		this.functions = new LinkedHashMap<String, Method>(functionsArg);
	}

	public Map<String, Method> getFunctions() {
		return this.functions;
	}

	protected void initialize(String beanName) {
		if (this.applicationContext != null) {
			conversionService();
			functions();
			propertyAccessors();
			processParentIfPresent(beanName);
		}
		this.initialized = true;
	}

	private void conversionService() {
		ConversionService conversionService = IntegrationUtils.getConversionService(getApplicationContext());
		if (conversionService != null) {
			this.typeConverter = new StandardTypeConverter(conversionService);
		}
	}

	private void functions() {
		Map<String, SpelFunctionFactoryBean> functionFactoryBeanMap = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.applicationContext, SpelFunctionFactoryBean.class);
		for (SpelFunctionFactoryBean spelFunctionFactoryBean : functionFactoryBeanMap.values()) {
			if (!getFunctions().containsKey(spelFunctionFactoryBean.getFunctionName())) {
				getFunctions().put(spelFunctionFactoryBean.getFunctionName(), spelFunctionFactoryBean.getObject());
			}
		}
	}

	private void propertyAccessors() {
		try {
			SpelPropertyAccessorRegistrar propertyAccessorRegistrar =
					this.applicationContext.getBean(SpelPropertyAccessorRegistrar.class);
			for (Entry<String, PropertyAccessor> entry : propertyAccessorRegistrar.getPropertyAccessors()
					.entrySet()) {
				if (!getPropertyAccessors().containsKey(entry.getKey())) {
					getPropertyAccessors().put(entry.getKey(), entry.getValue());
				}
			}
		}
		catch (@SuppressWarnings("unused") NoSuchBeanDefinitionException e) {
			// There is no 'SpelPropertyAccessorRegistrar' bean in the application context.
		}
	}

	private void processParentIfPresent(String beanName) {
		ApplicationContext parent = this.applicationContext.getParent();

		if (parent != null && parent.containsBean(beanName)) {
			AbstractEvaluationContextFactoryBean parentFactoryBean = parent.getBean("&" + beanName, getClass());

			for (Entry<String, PropertyAccessor> entry : parentFactoryBean.getPropertyAccessors().entrySet()) {
				if (!getPropertyAccessors().containsKey(entry.getKey())) {
					getPropertyAccessors().put(entry.getKey(), entry.getValue());
				}
			}

			for (Entry<String, Method> entry : parentFactoryBean.getFunctions().entrySet()) {
				if (!getFunctions().containsKey(entry.getKey())) {
					getFunctions().put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
