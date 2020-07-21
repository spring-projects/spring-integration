/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.groovy.config;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.scripting.config.ScriptExecutingProcessorFactory;

/**
 * The Groovy Module Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class GroovyIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final Log LOGGER = LogFactory.getLog(GroovyIntegrationConfigurationInitializer.class);

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerScriptExecutorProviderIfNecessary((BeanDefinitionRegistry) beanFactory);
		}
		else {
			LOGGER.warn("The 'ScriptExecutingProcessorFactory' isn't registered because 'beanFactory'" +
					" isn't an instance of `BeanDefinitionRegistry`.");
		}
	}

	protected void registerScriptExecutorProviderIfNecessary(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(ScriptExecutingProcessorFactory.BEAN_NAME)) {
			registry.registerBeanDefinition(ScriptExecutingProcessorFactory.BEAN_NAME,
					new RootBeanDefinition(GroovyAwareScriptExecutingProcessorFactory.class));
		}
	}

}
