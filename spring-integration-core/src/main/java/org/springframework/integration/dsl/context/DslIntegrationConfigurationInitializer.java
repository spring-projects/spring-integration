/*
 * Copyright 2016-2021 the original author or authors.
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
 * {@link org.springframework.integration.dsl.IntegrationComponentSpec#get()}.
 *
 * @author Artem Bilan
 * @author Gary Russell
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
					new RootBeanDefinition(IntegrationFlowBeanPostProcessor.class,
							IntegrationFlowBeanPostProcessor::new));
			registry.registerBeanDefinition(INTEGRATION_FLOW_CONTEXT_BEAN_NAME,
					new RootBeanDefinition(StandardIntegrationFlowContext.class, StandardIntegrationFlowContext::new));
			registry.registerBeanDefinition(INTEGRATION_FLOW_REPLY_PRODUCER_CLEANER_BEAN_NAME,
					new RootBeanDefinition(IntegrationFlowDefinition.ReplyProducerCleaner.class,
							IntegrationFlowDefinition.ReplyProducerCleaner::new));
		}
	}

}
