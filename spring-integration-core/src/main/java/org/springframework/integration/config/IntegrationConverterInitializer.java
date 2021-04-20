/*
 * Copyright 2014-2021 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
public class IntegrationConverterInitializer implements IntegrationConfigurationInitializer {

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		if (!registry.containsBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME,
					new RootBeanDefinition(ConverterRegistrar.class, ConverterRegistrar::new));
		}

		if (!registry.containsBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
					new RootBeanDefinition(CustomConversionServiceFactoryBean.class,
							CustomConversionServiceFactoryBean::new));
		}
	}

}
