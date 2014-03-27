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

package org.springframework.integration.jmx.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

/**
 * The JMX Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class JmxIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String MBEAN_EXPORTER_HELPER_BEAN_NAME = MBeanExporterHelper.class.getName();

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.registerMBeanExporterHelperIfNecessary(beanFactory);
	}

	private void registerMBeanExporterHelperIfNecessary(ConfigurableListableBeanFactory beanFactory) {
		if (beanFactory.getBeanNamesForType(IntegrationMBeanExporter.class).length > 0) {
			((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(MBEAN_EXPORTER_HELPER_BEAN_NAME,
					new RootBeanDefinition(MBeanExporterHelper.class));
		}
	}

}
