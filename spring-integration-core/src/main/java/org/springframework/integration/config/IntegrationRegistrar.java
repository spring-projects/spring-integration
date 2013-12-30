/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.config.annotation.EnableIntegration;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.expression.IntegrationEvaluationContextAwareBeanPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} implementation that configures integration infrastructure.
 * @author Artem Bilan
 * @since 4.0
 */
public class IntegrationRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		this.registerImplicitChannelCreator(registry);
		this.registerIntegrationEvaluationContext(registry);
		this.registerIntegrationProperties(registry);
		this.registerHeaderChannelRegistry(registry);
		this.registerBuiltInBeans(registry);
		this.registerDefaultConfiguringBeanFactoryPostProcessor(registry);
//		this.registerIntegrationFlowBeanFactoryPostProcessor(registry);
		if (importingClassMetadata != null) {
			this.registerMessagingAnnotationPostProcessors(importingClassMetadata, registry);
		}
	}

	/**
	 * This method will auto-register a ChannelInitializer which could also be overridden by the user
	 * by simply registering a ChannelInitializer {@code <bean>} with its {@code autoCreate} property
	 * set to false to suppress channel creation.
	 * It will also register a ChannelInitializer$AutoCreateCandidatesCollector which simply collects candidate channel names.
	 */
	private void registerImplicitChannelCreator(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME)) {
			String channelsAutoCreateExpression = IntegrationProperties.getExpressionFor(IntegrationProperties.CHANNELS_AUTOCREATE);
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".config.xml.ChannelInitializer")
					.addPropertyValue("autoCreate", channelsAutoCreateExpression);
			BeanDefinitionHolder channelCreatorHolder = new BeanDefinitionHolder(channelDef.getBeanDefinition(),
					IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelCreatorHolder, registry);
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME)) {
			BeanDefinitionBuilder channelRegistryBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".config.xml.ChannelInitializer$AutoCreateCandidatesCollector");
			channelRegistryBuilder.addConstructorArgValue(new ManagedSet<String>());
			BeanDefinitionHolder channelRegistryHolder = new BeanDefinitionHolder(channelRegistryBuilder.getBeanDefinition(),
					IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelRegistryHolder, registry);
		}
	}

	/**
	 * Register {@code integrationGlobalProperties} bean if necessary.
	 */
	private void registerIntegrationProperties(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry.isBeanNameInUse(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(this.classLoader);
			try {
				Resource[] defaultResources = resourceResolver.getResources("classpath*:META-INF/spring.integration.default.properties");
				Resource[] userResources = resourceResolver.getResources("classpath*:META-INF/spring.integration.properties");

				List<Resource> resources = new LinkedList<Resource>(Arrays.asList(defaultResources));
				resources.addAll(Arrays.asList(userResources));

				BeanDefinitionBuilder integrationPropertiesBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(PropertiesFactoryBean.class)
						.addPropertyValue("locations", resources);

				registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME,
						integrationPropertiesBuilder.getBeanDefinition());
			}
			catch (IOException e) {
				logger.warn("Cannot load 'spring.integration.properties' Resources.", e);
			}
		}
	}

	/**
	 * Register {@link IntegrationEvaluationContextFactoryBean} bean
	 * and {@link IntegrationEvaluationContextAwareBeanPostProcessor}, if necessary.
	 */
	private void registerIntegrationEvaluationContext(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)) {
			BeanDefinitionBuilder integrationEvaluationContextBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationEvaluationContextFactoryBean.class);
			integrationEvaluationContextBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			BeanDefinitionHolder integrationEvaluationContextHolder =
					new BeanDefinitionHolder(integrationEvaluationContextBuilder.getBeanDefinition(),
					IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME);

			BeanDefinitionReaderUtils.registerBeanDefinition(integrationEvaluationContextHolder,
					registry);

			RootBeanDefinition integrationEvalContextBPP = new RootBeanDefinition(IntegrationEvaluationContextAwareBeanPostProcessor.class);
			BeanDefinitionReaderUtils.registerWithGeneratedName(integrationEvalContextBPP, registry);
		}
	}

	/**
	 * Register {@code jsonPath} and {@code xpath} SpEL-function beans, if necessary.
	 */
	private void registerBuiltInBeans(BeanDefinitionRegistry registry) {
		String jsonPathBeanName = "jsonPath";
		if (!registry.containsBeanDefinition(jsonPathBeanName)) {
			Class<?> jsonPathClass = null;
			try {
				jsonPathClass = ClassUtils.forName("com.jayway.jsonpath.JsonPath", this.classLoader);
			}
			catch (ClassNotFoundException e) {
				logger.debug("SpEL function '#jsonPath' isn't registered: there is no jayway json-path.jar on the classpath.");
			}

			if (jsonPathClass != null) {
				IntegrationNamespaceUtils.registerSpelFunctionBean(registry, jsonPathBeanName,
						IntegrationNamespaceUtils.BASE_PACKAGE + ".json.JsonPathUtils", "evaluate");
			}
		}

		String xpathBeanName = "xpath";
		if (!registry.containsBeanDefinition(xpathBeanName)) {
			Class<?> xpathClass = null;
			try {
				xpathClass = ClassUtils.forName(IntegrationNamespaceUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils",
						this.classLoader);
			}
			catch (ClassNotFoundException e) {
				logger.debug("SpEL function '#xpath' isn't registered: there is no spring-integration-xml.jar on the classpath.");
			}

			if (xpathClass != null) {
				IntegrationNamespaceUtils.registerSpelFunctionBean(registry, xpathBeanName,
						IntegrationNamespaceUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils", "evaluate");
			}
		}
	}

	/**
	 * Register {@code DefaultConfiguringBeanFactoryPostProcessor}, if necessary.
	 */
	private void registerDefaultConfiguringBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry).containsBean(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry.isBeanNameInUse(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".config.xml.DefaultConfiguringBeanFactoryPostProcessor");
			BeanDefinitionHolder postProcessorHolder = new BeanDefinitionHolder(
					postProcessorBuilder.getBeanDefinition(), IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(postProcessorHolder, registry);
		}
	}

	/**
	 * Register a {@link DefaultHeaderChannelRegistry} in the given {@link BeanDefinitionRegistry}, if necessary.
	 */
	private void registerHeaderChannelRegistry(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry.isBeanNameInUse(
					IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			if (logger.isInfoEnabled()) {
				logger.info("No bean named '" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME +
						"' has been explicitly defined. Therefore, a default DefaultHeaderChannelRegistry will be created.");
			}
			BeanDefinitionBuilder schedulerBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultHeaderChannelRegistry.class);
			BeanDefinitionHolder replyChannelRegistryComponent = new BeanDefinitionHolder(
					schedulerBuilder.getBeanDefinition(),
					IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(replyChannelRegistryComponent, registry);
		}
	}

	/**
	 * Register {@link MessagingAnnotationPostProcessor} and {@link PublisherAnnotationBeanPostProcessor}, if necessary.
	 * Inject {@code defaultPublishedChannel} from provided {@link AnnotationMetadata}, if any.
	 */
	private void registerMessagingAnnotationPostProcessors(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessagingAnnotationPostProcessor.class)
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME, builder.getBeanDefinition());
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class)
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			Map<String, Object> attrs = meta.getAnnotationAttributes(EnableIntegration.class.getName());

			String defaultPublishedChannel = (String) attrs.get("defaultPublishedChannel");
			if (StringUtils.hasText(defaultPublishedChannel)) {
				builder.addPropertyReference("defaultChannel", defaultPublishedChannel);
			}

			registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME, builder.getBeanDefinition());
		}

	}

	/**
	 * Register {@link IntegrationFlowBeanFactoryPostProcessor} to process Integration Java DSL.
	 */
	/*private void registerIntegrationFlowBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.FLOW_POST_PROCESSOR_BEAN_NAME)) {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationFlowBeanFactoryPostProcessor.class)
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(IntegrationContextUtils.FLOW_POST_PROCESSOR_BEAN_NAME, postProcessorBuilder.getBeanDefinition());
		}
	}*/

}
