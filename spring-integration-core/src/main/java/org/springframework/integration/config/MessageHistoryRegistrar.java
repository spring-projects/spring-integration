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
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.history.MessageHistoryConfigurer;

/**
 * Registers the {@link MessageHistoryConfigurer} {@link org.springframework.beans.factory.config.BeanDefinition}
 * for {@link org.springframework.integration.history.MessageHistory}.
 * This registrar is applied from {@code @EnableMessageHistory} on the {@code Configuration} class
 * or from {@code MessageHistoryParser}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class MessageHistoryRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME)) {
			throw new BeanDefinitionStoreException(
					"Only one @EnableMessageHistory or <message-history/> can be declared in the application context.");
		}

		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnableMessageHistory.class.getName());
		Object componentNamePatterns = annotationAttributes.get("value"); // NOSONAR never null

		String patterns;

		if (componentNamePatterns instanceof String[]) {
			patterns = String.join(",", (String[]) componentNamePatterns);
		}
		else {
			patterns = (String) componentNamePatterns;
		}

		BeanDefinition messageHistoryConfigurer =
				BeanDefinitionBuilder.genericBeanDefinition(MessageHistoryConfigurer.class)
						.addPropertyValue("componentNamePatterns", patterns)
						.getBeanDefinition();

		registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME,
				messageHistoryConfigurer);
	}

}
