/*
 * Copyright 2014-2021 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} implementation that configures integration infrastructure.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class IntegrationRegistrar implements ImportBeanDefinitionRegistrar {

	static {
		if (ClassUtils.isPresent("org.springframework.integration.dsl.support.Function", null)) {
			throw new ApplicationContextException("Starting with Spring Integration 5.0, "
					+ "the 'spring-integration-java-dsl' dependency is no longer needed; "
					+ "the Java DSL has been merged into the core project. "
					+ "If it is present on the classpath, it will cause class loading conflicts.");
		}
	}

	/**
	 * Invoked by the framework when an &#64;EnableIntegration annotation is encountered.
	 * Also called with {@code null} {@code importingClassMetadata} from {@code AbstractIntegrationNamespaceHandler}
	 * to register the same beans when using XML configuration. Also called by {@code AnnotationConfigParser}
	 * to register the messaging annotation post processors (for {@code <int:annotation-config/>}).
	 */
	@Override
	public void registerBeanDefinitions(@Nullable AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {

		registerDefaultConfiguringBeanFactoryPostProcessor(registry);
		registerIntegrationConfigurationBeanFactoryPostProcessor(registry);
		if (importingClassMetadata != null) {
			registerMessagingAnnotationPostProcessors(registry);
		}
	}

	/**
	 * Register {@code DefaultConfiguringBeanFactoryPostProcessor}, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerDefaultConfiguringBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME,
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class,
							DefaultConfiguringBeanFactoryPostProcessor::new));
		}
	}

	/**
	 * Register {@link IntegrationConfigurationBeanFactoryPostProcessor}
	 * to process the external Integration infrastructure.
	 */
	private void registerIntegrationConfigurationBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(
				IntegrationContextUtils.INTEGRATION_CONFIGURATION_POST_PROCESSOR_BEAN_NAME)) {

			BeanDefinitionBuilder postProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationConfigurationBeanFactoryPostProcessor.class,
							IntegrationConfigurationBeanFactoryPostProcessor::new)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_CONFIGURATION_POST_PROCESSOR_BEAN_NAME,
					postProcessorBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register {@link MessagingAnnotationPostProcessor} and
	 * {@link org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor},
	 * if necessary.
	 * Inject {@code defaultPublishedChannel} from provided {@link AnnotationMetadata}, if any.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerMessagingAnnotationPostProcessors(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(MessagingAnnotationPostProcessor.class,
							MessagingAnnotationPostProcessor::new)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME,
					builder.getBeanDefinition());
		}
	}

}
