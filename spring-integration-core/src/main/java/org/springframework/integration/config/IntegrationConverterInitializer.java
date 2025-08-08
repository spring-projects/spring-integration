/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
			RootBeanDefinition beanDefinition = new RootBeanDefinition(CustomConversionServiceFactoryBean.class);
			beanDefinition.setAutowireCandidate(false);
			registry.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
					beanDefinition);
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
