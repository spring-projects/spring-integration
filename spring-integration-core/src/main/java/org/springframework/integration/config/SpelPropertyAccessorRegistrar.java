/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.config;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.PropertyAccessor;

/**
 * Utility class that keeps track of a Set of SpEL {@link PropertyAccessor}s
 * in order to register them with the "integrationEvaluationContext" upon initialization.
 *
 * @author Artem Bilan
 * @since 3.0
 */
class SpelPropertyAccessorRegistrar implements ApplicationContextAware, InitializingBean {

	private final Map<String, PropertyAccessor> propertyAccessors;

	private ApplicationContext applicationContext;

	SpelPropertyAccessorRegistrar(Map<String, PropertyAccessor> propertyAccessors) {
		this.propertyAccessors = propertyAccessors;
	}

	Collection<PropertyAccessor> getPropertyAccessors() {
		return propertyAccessors.values();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SpelPropertyAccessorRegistrar parentPropertyAccessorRegistrar = null;
		try {
			BeanFactory parentBeanFactory = this.applicationContext.getParentBeanFactory();
			if (parentBeanFactory != null) {
				parentPropertyAccessorRegistrar = parentBeanFactory.getBean(SpelPropertyAccessorRegistrar.class);
			}
		}
		catch (NoSuchBeanDefinitionException e) {
			// There is no 'SpelPropertyAccessorRegistrar' bean with the parent application context
			// Ignore it
		}

		if (parentPropertyAccessorRegistrar != null) {
			for (Map.Entry<String, PropertyAccessor> entry : parentPropertyAccessorRegistrar.propertyAccessors.entrySet()) {
				if (!this.propertyAccessors.containsKey(entry.getKey())) {
					this.propertyAccessors.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
