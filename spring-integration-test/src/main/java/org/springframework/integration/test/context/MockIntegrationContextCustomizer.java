/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.test.context;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * The {@link ContextCustomizer} implementation for Spring Integration specific environment.
 * <p>
 * Registers {@link MockIntegrationContext} bean.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class MockIntegrationContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory);
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		registry.registerBeanDefinition(MockIntegrationContext.MOCK_INTEGRATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(MockIntegrationContext.class));
	}

	@Override
	public int hashCode() {
		return MockIntegrationContextCustomizer.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass();
	}

}

