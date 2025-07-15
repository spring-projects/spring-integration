/*
 * Copyright 2018-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.IndexAccessor;
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

	private Map<String, PropertyAccessor> propertyAccessors = new LinkedHashMap<>();

	private Map<String, IndexAccessor> indexAccessors = new LinkedHashMap<>();

	private Map<String, Method> functions = new LinkedHashMap<>();

	private TypeConverter typeConverter = new StandardTypeConverter();

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	@Nullable
	private SpelPropertyAccessorRegistrar propertyAccessorRegistrar;

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

	public void setPropertyAccessors(Map<String, PropertyAccessor> propertyAccessors) {
		Assert.isTrue(!this.initialized, "'propertyAccessors' can't be changed after initialization.");
		Assert.notNull(propertyAccessors, "'propertyAccessors' must not be null.");
		Assert.noNullElements(propertyAccessors.values().toArray(), "'propertyAccessors' cannot have null values.");
		this.propertyAccessors = new LinkedHashMap<>(propertyAccessors);
	}

	public Map<String, PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	/**
	 * Set a map of {@link IndexAccessor}s to use in the target {@link org.springframework.expression.EvaluationContext}
	 * @param indexAccessors the map of {@link IndexAccessor}s to use
	 * @since 6.4
	 * @see org.springframework.expression.EvaluationContext#getIndexAccessors()
	 */
	public void setIndexAccessors(Map<String, IndexAccessor> indexAccessors) {
		Assert.isTrue(!this.initialized, "'indexAccessors' can't be changed after initialization.");
		Assert.notNull(indexAccessors, "'indexAccessors' must not be null.");
		Assert.noNullElements(indexAccessors.values().toArray(), "'indexAccessors' cannot have null values.");
		this.indexAccessors = new LinkedHashMap<>(indexAccessors);
	}

	/**
	 * Return the map of {@link IndexAccessor}s to use in the target {@link org.springframework.expression.EvaluationContext}
	 * @return the map of {@link IndexAccessor}s to use
	 * @since 6.4
	 * @see org.springframework.expression.EvaluationContext#getIndexAccessors()
	 */
	public Map<String, IndexAccessor> getIndexAccessors() {
		return this.indexAccessors;
	}

	public void setFunctions(Map<String, Method> functionsArg) {
		Assert.isTrue(!this.initialized, "'functions' can't be changed after initialization.");
		Assert.notNull(functionsArg, "'functions' must not be null.");
		Assert.noNullElements(functionsArg.values().toArray(), "'functions' cannot have null values.");
		this.functions = new LinkedHashMap<>(functionsArg);
	}

	public Map<String, Method> getFunctions() {
		return this.functions;
	}

	protected void initialize(String beanName) {
		if (this.applicationContext != null) {
			conversionService();
			functions();
			try {
				this.propertyAccessorRegistrar = this.applicationContext.getBean(SpelPropertyAccessorRegistrar.class);
			}
			catch (@SuppressWarnings("unused") NoSuchBeanDefinitionException e) {
				// There is no 'SpelPropertyAccessorRegistrar' bean in the application context.
			}
			propertyAccessors();
			indexAccessors();
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
		Map<String, SpelFunctionFactoryBean> spelFunctions =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, SpelFunctionFactoryBean.class);
		for (SpelFunctionFactoryBean spelFunctionFactoryBean : spelFunctions.values()) {
			String functionName = spelFunctionFactoryBean.getFunctionName();
			if (!this.functions.containsKey(functionName)) {
				this.functions.put(functionName, spelFunctionFactoryBean.getObject());
			}
		}
	}

	private void propertyAccessors() {
		if (this.propertyAccessorRegistrar != null) {
			propertyAccessors(this.propertyAccessorRegistrar.getPropertyAccessors());
		}
	}

	private void propertyAccessors(Map<String, PropertyAccessor> propertyAccessors) {
		for (Entry<String, PropertyAccessor> entry : propertyAccessors.entrySet()) {
			String key = entry.getKey();
			if (!this.propertyAccessors.containsKey(key)) {
				this.propertyAccessors.put(key, entry.getValue());
			}
		}
	}

	private void indexAccessors() {
		if (this.propertyAccessorRegistrar != null) {
			indexAccessors(this.propertyAccessorRegistrar.getIndexAccessors());
		}
	}

	private void indexAccessors(Map<String, IndexAccessor> indexAccessors) {
		for (Entry<String, IndexAccessor> entry : indexAccessors.entrySet()) {
			String key = entry.getKey();
			if (!this.indexAccessors.containsKey(key)) {
				this.indexAccessors.put(key, entry.getValue());
			}
		}
	}

	private void processParentIfPresent(String beanName) {
		ApplicationContext parent = this.applicationContext.getParent();

		if (parent != null && parent.containsBean(beanName)) {
			AbstractEvaluationContextFactoryBean parentFactoryBean = parent.getBean("&" + beanName, getClass());
			propertyAccessors(parentFactoryBean.getPropertyAccessors());
			indexAccessors(parentFactoryBean.getIndexAccessors());
			for (Entry<String, Method> entry : parentFactoryBean.getFunctions().entrySet()) {
				String key = entry.getKey();
				if (!this.functions.containsKey(key)) {
					this.functions.put(key, entry.getValue());
				}
			}
		}
	}

}
