/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.hazelcast.HazelcastLocalInstanceRegistrar;

/**
 * The Hazelcast Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
		if (!beanDefinitionRegistry.containsBeanDefinition(HazelcastLocalInstanceRegistrar.BEAN_NAME)) {
			beanDefinitionRegistry.registerBeanDefinition(HazelcastLocalInstanceRegistrar.BEAN_NAME,
					new RootBeanDefinition(HazelcastLocalInstanceRegistrar.class));
		}
	}

}
