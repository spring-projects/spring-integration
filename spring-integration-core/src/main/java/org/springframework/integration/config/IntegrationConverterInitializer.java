/*
 * Copyright 2014-2023 the original author or authors.
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
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;

/**
 * The {@link IntegrationConfigurationInitializer} to populate
 * {@link ConverterRegistrar.IntegrationConverterRegistration}
 * for converter beans marked with an {@link IntegrationConverter} annotation.
 * <p>
 * {@link org.springframework.context.annotation.Bean} methods are also processed.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class IntegrationConverterInitializer implements IntegrationConfigurationInitializer {

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (isIntegrationConverter(beanDefinition)) {
				BeanDefinitionBuilder builder =
						BeanDefinitionBuilder.genericBeanDefinition(
										ConverterRegistrar.IntegrationConverterRegistration.class)
								.addConstructorArgReference(beanName);
				BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
			}
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME,
					new RootBeanDefinition(ConverterRegistrar.class));
		}

		if (!registry.containsBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
					new RootBeanDefinition(CustomConversionServiceFactoryBean.class));
		}

	}

	private static boolean isIntegrationConverter(BeanDefinition beanDefinition) {
		boolean hasIntegrationConverter = false;
		if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
			AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
			hasIntegrationConverter = metadata.hasAnnotation(IntegrationConverter.class.getName());
			if (!hasIntegrationConverter && beanDefinition.getSource() instanceof MethodMetadata beanMethodMetadata) {
				hasIntegrationConverter = beanMethodMetadata.isAnnotated(IntegrationConverter.class.getName());
			}
		}
		return hasIntegrationConverter;
	}

}
