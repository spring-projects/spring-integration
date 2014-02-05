/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} to apply external Integration infrastructure configurations
 * via loading {@link IntegrationConfigurationInitializer} implementations using {@link SpringFactoriesLoader}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class IntegrationConfigurationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Set<String> initializerNames = new HashSet<String>(
				SpringFactoriesLoader.loadFactoryNames(IntegrationConfigurationInitializer.class, beanFactory.getBeanClassLoader()));

		for (String initializerName : initializerNames) {
			try {
				Class<?> instanceClass = ClassUtils.forName(initializerName, beanFactory.getBeanClassLoader());
				Assert.isAssignable(IntegrationConfigurationInitializer.class, instanceClass);
				IntegrationConfigurationInitializer instance = (IntegrationConfigurationInitializer) instanceClass.newInstance();
				instance.initialize(beanFactory);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Cannot instantiate 'IntegrationConfigurationInitializer': " + initializerName, e);
			}
		}
	}

}
