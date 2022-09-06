/*
 * Copyright 2014-2022 the original author or authors.
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

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * {@link BeanDefinitionRegistryPostProcessor} to apply external Integration
 * infrastructure configurations via loading {@link IntegrationConfigurationInitializer}
 * implementations using {@link SpringFactoriesLoader}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class IntegrationConfigurationBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;

		List<IntegrationConfigurationInitializer> initializers =
				SpringFactoriesLoader.loadFactories(IntegrationConfigurationInitializer.class,
						beanFactory.getBeanClassLoader());

		for (IntegrationConfigurationInitializer initializer : initializers) {
			initializer.initialize(beanFactory);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

}
