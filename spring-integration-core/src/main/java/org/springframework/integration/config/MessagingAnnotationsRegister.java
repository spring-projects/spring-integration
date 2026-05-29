/*
 * Copyright 2026-present the original author or authors.
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

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * Registers {@link org.springframework.beans.factory.config.BeanDefinition}
 * for messaging annotations post-processors.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 *
 * @see MessagingAnnotationPostProcessor
 * @see MessagingAnnotationBeanPostProcessor
 * @see GatewayProxyInstantiationPostProcessor
 */
public class MessagingAnnotationsRegister implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		registerMessagingAnnotationPostProcessors(registry);
		registerGatewayProxyInstantiationPostProcessor(registry);
	}

	/**
	 * Register {@link MessagingAnnotationPostProcessor} and
	 * {@link MessagingAnnotationBeanPostProcessor},
	 * if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 * @see MessagingAnnotationPostProcessor#messagingAnnotationBeanPostProcessor()
	 */
	private void registerMessagingAnnotationPostProcessors(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME,
					BeanDefinitionBuilder.genericBeanDefinition(MessagingAnnotationPostProcessor.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
							.getBeanDefinition());
		}

		String beanName = Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName());
		if (!registry.containsBeanDefinition(beanName)) {
			registry.registerBeanDefinition(beanName,
					BeanDefinitionBuilder.genericBeanDefinition()
							.setFactoryMethodOnBean("messagingAnnotationBeanPostProcessor",
									IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
							.getBeanDefinition());
		}
	}

	private void registerGatewayProxyInstantiationPostProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition("gatewayProxyBeanDefinitionPostProcessor")) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(GatewayProxyInstantiationPostProcessor.class)
							.addConstructorArgValue(registry)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition("gatewayProxyBeanDefinitionPostProcessor", builder.getBeanDefinition());
		}
	}

}
