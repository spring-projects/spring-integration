/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.aot;

import java.util.Arrays;

import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.integration.config.DefaultConfiguringBeanFactoryPostProcessor;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;
import org.springframework.integration.config.IntegrationConfigurationBeanFactoryPostProcessor;

/**
 * The {@link BeanRegistrationExcludeFilter} to exclude beans not need at runtime anymore.
 * Usually {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} beans
 * are excluded since they have contributed their bean definitions code generated during AOT
 * build phase.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class IntegrationBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		return Arrays.asList(DefaultConfiguringBeanFactoryPostProcessor.class,
						IntegrationConfigurationBeanFactoryPostProcessor.class,
						IntegrationComponentScanRegistrar.class)
				.contains(beanClass);
	}

}
