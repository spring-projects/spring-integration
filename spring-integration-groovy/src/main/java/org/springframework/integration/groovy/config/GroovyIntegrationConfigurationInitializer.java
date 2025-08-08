/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.scripting.config.ScriptExecutingProcessorFactory;

/**
 * The Groovy Module Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 5.0
 */
public class GroovyIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final Log LOGGER = LogFactory.getLog(GroovyIntegrationConfigurationInitializer.class);

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerScriptExecutorProviderIfNecessary((BeanDefinitionRegistry) beanFactory);
		}
		else {
			LOGGER.warn("The 'ScriptExecutingProcessorFactory' isn't registered because 'beanFactory'" +
					" isn't an instance of `BeanDefinitionRegistry`.");
		}
	}

	protected void registerScriptExecutorProviderIfNecessary(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(ScriptExecutingProcessorFactory.BEAN_NAME)) {
			registry.registerBeanDefinition(ScriptExecutingProcessorFactory.BEAN_NAME,
					new RootBeanDefinition(GroovyAwareScriptExecutingProcessorFactory.class));
		}
	}

}
