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

package org.springframework.integration.config.annotation;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

/**
 * A {@link ConfigurationClassPostProcessor} implementation to register integration annotated
 * components only once globally, in root {@link org.springframework.context.ApplicationContext}.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class IntegrationConfigurationClassPostProcessor extends ConfigurationClassPostProcessor {

	private volatile boolean processed;

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		if (!registry.isBeanNameInUse(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			super.postProcessBeanDefinitionRegistry(registry);
		}
		processed = true;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		if (!this.processed) {
			super.postProcessBeanFactory(beanFactory);
		}
	}

}
