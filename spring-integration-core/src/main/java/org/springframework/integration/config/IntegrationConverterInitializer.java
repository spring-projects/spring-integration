/*
 * Copyright 2014 the original author or authors.
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

import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class IntegrationConverterInitializer implements IntegrationConfigurationInitializer {

	private static final String CONTEXT_PACKAGE = "org.springframework.integration.context.";

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (beanDefinition instanceof AnnotatedBeanDefinition) {
				AnnotationMetadata metadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
				boolean hasIntegrationConverter = metadata.hasAnnotation(IntegrationConverter.class.getName());

				if (!hasIntegrationConverter && beanDefinition.getSource() instanceof MethodMetadata) {
					MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
					hasIntegrationConverter = beanMethod.isAnnotated(IntegrationConverter.class.getName());
				}

				if (hasIntegrationConverter) {
					this.registerConverter(registry, new RuntimeBeanReference(beanName));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void registerConverter(BeanDefinitionRegistry registry, BeanMetadataElement converterBeanDefinition) {
		Set<BeanMetadataElement> converters = new ManagedSet<BeanMetadataElement>();
		if (!registry.containsBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME)) {
			BeanDefinitionBuilder converterRegistrarBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					CONTEXT_PACKAGE + "ConverterRegistrar").addConstructorArgValue(converters);
			registry.registerBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME,
					converterRegistrarBuilder.getBeanDefinition());

			if (!registry.containsBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)) {
				registry.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
						new RootBeanDefinition(CONTEXT_PACKAGE + "CustomConversionServiceFactoryBean"));
			}
		}
		else {
			BeanDefinition converterRegistrarBeanDefinition = registry
					.getBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME);
			converters = (Set<BeanMetadataElement>) converterRegistrarBeanDefinition
					.getConstructorArgumentValues()
					.getIndexedArgumentValues()
					.values()
					.iterator()
					.next()
					.getValue();
		}

		converters.add(converterBeanDefinition);
	}
}
