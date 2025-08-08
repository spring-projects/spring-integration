/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class PublisherRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			throw new BeanDefinitionStoreException("Only one enable publisher definition " +
					"(@EnablePublisher or <annotation-config>) can be declared in the application context.");
		}
		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnablePublisher.class.getName());

		String defaultChannel =
				annotationAttributes == null
						? (String) AnnotationUtils.getDefaultValue(EnablePublisher.class)
						: (String) annotationAttributes.get("defaultChannel");

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		if (StringUtils.hasText(defaultChannel)) {
			builder.addPropertyValue("defaultChannelName", defaultChannel);
		}

		if (annotationAttributes != null) {
			builder.addPropertyValue("proxyTargetClass", annotationAttributes.get("proxyTargetClass"))
					.addPropertyValue("order", annotationAttributes.get("order"));
		}
		registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME,
				builder.getBeanDefinition());
	}

}
