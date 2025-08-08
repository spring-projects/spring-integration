/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl.context;

import java.beans.Introspector;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.util.Assert;

/**
 * The Java DSL Integration infrastructure {@code beanFactory} initializer.
 * Registers {@link IntegrationFlowBeanPostProcessor} and checks if all
 * {@link org.springframework.integration.dsl.IntegrationComponentSpec} are extracted to
 * the target object using
 * {@link org.springframework.integration.dsl.IntegrationComponentSpec#getObject()}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 5.0
 *
 * @see org.springframework.integration.config.IntegrationConfigurationBeanFactoryPostProcessor
 */
public class DslIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String INTEGRATION_FLOW_BPP_BEAN_NAME =
			Introspector.decapitalize(IntegrationFlowBeanPostProcessor.class.getName());

	private static final String INTEGRATION_FLOW_CONTEXT_BEAN_NAME =
			Introspector.decapitalize(IntegrationFlowContext.class.getName());

	private static final String INTEGRATION_FLOW_REPLY_PRODUCER_CLEANER_BEAN_NAME =
			Introspector.decapitalize(IntegrationFlowDefinition.ReplyProducerCleaner.class.getName());

	@Override
	public void initialize(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, configurableListableBeanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'BeanDefinitionRegistry'. Consider using 'GenericApplicationContext' implementation."
		);

		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableListableBeanFactory;
		if (!registry.containsBeanDefinition(INTEGRATION_FLOW_BPP_BEAN_NAME)) {
			registry.registerBeanDefinition(INTEGRATION_FLOW_BPP_BEAN_NAME,
					new RootBeanDefinition(IntegrationFlowBeanPostProcessor.class));
			registry.registerBeanDefinition(INTEGRATION_FLOW_CONTEXT_BEAN_NAME,
					new RootBeanDefinition(StandardIntegrationFlowContext.class));
			registry.registerBeanDefinition(INTEGRATION_FLOW_REPLY_PRODUCER_CLEANER_BEAN_NAME,
					new RootBeanDefinition(IntegrationFlowDefinition.ReplyProducerCleaner.class));
		}
	}

}
