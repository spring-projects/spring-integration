/*
 * Copyright 2014-2018 the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.handler.support.HandlerMethodArgumentResolversHolder;
import org.springframework.integration.handler.support.MapArgumentResolver;
import org.springframework.integration.handler.support.PayloadExpressionArgumentResolver;
import org.springframework.integration.handler.support.PayloadsArgumentResolver;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} implementation that configures integration infrastructure.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class IntegrationRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(IntegrationRegistrar.class);

	private final static IntegrationConverterInitializer INTEGRATION_CONVERTER_INITIALIZER =
			new IntegrationConverterInitializer();

	private static final Set<Integer> registriesProcessed = new HashSet<>();

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
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

		registerImplicitChannelCreator(registry);
		registerIntegrationConfigurationBeanFactoryPostProcessor(registry);
		registerIntegrationEvaluationContext(registry);
		registerIntegrationProperties(registry);
		registerHeaderChannelRegistry(registry);
		registerGlobalChannelInterceptorProcessor(registry);
		registerBuiltInBeans(registry);
		registerDefaultConfiguringBeanFactoryPostProcessor(registry);
		registerDefaultDatatypeChannelMessageConverter(registry);
		registerArgumentResolverMessageConverter(registry);
		registerArgumentResolvers(registry);
		registerListCapableArgumentResolvers(registry);
		if (importingClassMetadata != null) {
			registerMessagingAnnotationPostProcessors(importingClassMetadata, registry);
		}
		registerMessageBuilderFactory(registry);
		IntegrationConfigUtils.registerRoleControllerDefinitionIfNecessary(registry);
	}

	/**
	 * This method will auto-register a ChannelInitializer which could also be overridden by the user
	 * by simply registering a ChannelInitializer {@code <bean>} with its {@code autoCreate} property
	 * set to false to suppress channel creation.
	 * It will also register a ChannelInitializer$AutoCreateCandidatesCollector
	 * which simply collects candidate channel names.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerImplicitChannelCreator(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME)) {
			String channelsAutoCreateExpression =
					IntegrationProperties.getExpressionFor(IntegrationProperties.CHANNELS_AUTOCREATE);
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(ChannelInitializer.class)
					.addPropertyValue("autoCreate", channelsAutoCreateExpression);
			BeanDefinitionHolder channelCreatorHolder = new BeanDefinitionHolder(channelDef.getBeanDefinition(),
					IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelCreatorHolder, registry);
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME)) {
			BeanDefinitionBuilder channelRegistryBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ChannelInitializer.AutoCreateCandidatesCollector.class);
			channelRegistryBuilder.addConstructorArgValue(new ManagedSet<String>());
			channelRegistryBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE); //SPR-12761
			BeanDefinitionHolder channelRegistryHolder =
					new BeanDefinitionHolder(channelRegistryBuilder.getBeanDefinition(),
							IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelRegistryHolder, registry);
		}
	}

	/**
	 * Register {@code integrationGlobalProperties} bean if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerIntegrationProperties(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME);
		}
		else {
			alreadyRegistered =
					registry.isBeanNameInUse(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(this.classLoader);
			try {
				Resource[] defaultResources =
						resourceResolver.getResources("classpath*:META-INF/spring.integration.default.properties");
				Resource[] userResources =
						resourceResolver.getResources("classpath*:META-INF/spring.integration.properties");

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
	 * Register {@link IntegrationEvaluationContextFactoryBean} bean, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerIntegrationEvaluationContext(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)) {
			BeanDefinitionBuilder integrationEvaluationContextBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationEvaluationContextFactoryBean.class);
			integrationEvaluationContextBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			BeanDefinitionHolder integrationEvaluationContextHolder =
					new BeanDefinitionHolder(integrationEvaluationContextBuilder.getBeanDefinition(),
							IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME);

			BeanDefinitionReaderUtils.registerBeanDefinition(integrationEvaluationContextHolder, registry);
		}
		if (!registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME)) {
			BeanDefinitionBuilder integrationEvaluationContextBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationSimpleEvaluationContextFactoryBean.class);
			integrationEvaluationContextBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			BeanDefinitionHolder integrationEvaluationContextHolder =
					new BeanDefinitionHolder(integrationEvaluationContextBuilder.getBeanDefinition(),
							IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME);

			BeanDefinitionReaderUtils.registerBeanDefinition(integrationEvaluationContextHolder, registry);
		}
	}

	/**
	 * Register {@code jsonPath} and {@code xpath} SpEL-function beans, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerBuiltInBeans(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);

		String jsonPathBeanName = "jsonPath";
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry).containsBean(jsonPathBeanName);
		}
		else {
			alreadyRegistered = registry.isBeanNameInUse(jsonPathBeanName);
		}
		if (!alreadyRegistered && !registriesProcessed.contains(registryId)) {
			Class<?> jsonPathClass = null;
			try {
				jsonPathClass = ClassUtils.forName("com.jayway.jsonpath.JsonPath", this.classLoader);
			}
			catch (ClassNotFoundException e) {
				logger.debug("The '#jsonPath' SpEL function cannot be registered: " +
						"there is no jayway json-path.jar on the classpath.");
			}

			if (jsonPathClass != null) {
				try {
					ClassUtils.forName("com.jayway.jsonpath.Predicate", this.classLoader);
				}
				catch (ClassNotFoundException e) {
					jsonPathClass = null;
					logger.warn("The '#jsonPath' SpEL function cannot be registered. " +
							"An old json-path.jar version is detected in the classpath." +
							"At least 1.2.0 is required; see version information at: " +
							"https://github.com/jayway/JsonPath/releases", e);

				}
			}

			if (jsonPathClass != null) {
				IntegrationConfigUtils.registerSpelFunctionBean(registry, jsonPathBeanName,
						IntegrationConfigUtils.BASE_PACKAGE + ".json.JsonPathUtils", "evaluate");
			}
		}

		alreadyRegistered = false;
		String xpathBeanName = "xpath";
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry).containsBean(xpathBeanName);
		}
		else {
			alreadyRegistered = registry.isBeanNameInUse(xpathBeanName);
		}
		if (!alreadyRegistered && !registriesProcessed.contains(registryId)) {
			Class<?> xpathClass = null;
			try {
				xpathClass = ClassUtils.forName(IntegrationConfigUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils",
						this.classLoader);
			}
			catch (ClassNotFoundException e) {
				logger.debug("SpEL function '#xpath' isn't registered: " +
						"there is no spring-integration-xml.jar on the classpath.");
			}

			if (xpathClass != null) {
				IntegrationConfigUtils.registerSpelFunctionBean(registry, xpathBeanName,
						IntegrationConfigUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils", "evaluate");
			}
		}

		alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.TO_STRING_FRIENDLY_JSON_NODE_TO_STRING_CONVERTER_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry
					.isBeanNameInUse(IntegrationContextUtils.TO_STRING_FRIENDLY_JSON_NODE_TO_STRING_CONVERTER_BEAN_NAME);
		}

		if (!alreadyRegistered && !registriesProcessed.contains(registryId) && JacksonPresent.isJackson2Present()) {
			registry.registerBeanDefinition(
					IntegrationContextUtils.TO_STRING_FRIENDLY_JSON_NODE_TO_STRING_CONVERTER_BEAN_NAME,
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationConfigUtils.BASE_PACKAGE +
							".json.ToStringFriendlyJsonNodeToStringConverter")
							.getBeanDefinition());
			INTEGRATION_CONVERTER_INITIALIZER.registerConverter(registry,
					new RuntimeBeanReference(
							IntegrationContextUtils.TO_STRING_FRIENDLY_JSON_NODE_TO_STRING_CONVERTER_BEAN_NAME));
		}

		registriesProcessed.add(registryId);
	}

	/**
	 * Register {@code DefaultConfiguringBeanFactoryPostProcessor}, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerDefaultConfiguringBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		else {
			alreadyRegistered =
					registry.isBeanNameInUse(IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder postProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class);
			BeanDefinitionHolder postProcessorHolder = new BeanDefinitionHolder(
					postProcessorBuilder.getBeanDefinition(),
					IntegrationContextUtils.DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(postProcessorHolder, registry);
		}
	}

	/**
	 * Register a {@link DefaultHeaderChannelRegistry} in the given {@link BeanDefinitionRegistry}, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerHeaderChannelRegistry(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
		}
		else {
			alreadyRegistered =
					registry.isBeanNameInUse(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			if (logger.isInfoEnabled()) {
				logger.info("No bean named '" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME +
						"' has been explicitly defined. " +
						"Therefore, a default DefaultHeaderChannelRegistry will be created.");
			}
			BeanDefinitionBuilder schedulerBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultHeaderChannelRegistry.class);
			BeanDefinitionHolder replyChannelRegistryComponent = new BeanDefinitionHolder(
					schedulerBuilder.getBeanDefinition(),
					IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(replyChannelRegistryComponent, registry);
		}
	}

	/**
	 * Register a {@link GlobalChannelInterceptorProcessor} in the given {@link BeanDefinitionRegistry}, if necessary.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerGlobalChannelInterceptorProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_PROCESSOR_BEAN_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorProcessor.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_PROCESSOR_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	/**
	 * Register {@link MessagingAnnotationPostProcessor} and {@link PublisherAnnotationBeanPostProcessor}, if necessary.
	 * Inject {@code defaultPublishedChannel} from provided {@link AnnotationMetadata}, if any.
	 * @param meta The {@link AnnotationMetadata} to get additional properties for {@link BeanDefinition}s.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private void registerMessagingAnnotationPostProcessors(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(MessagingAnnotationPostProcessor.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			registry.registerBeanDefinition(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME,
					builder.getBeanDefinition());
		}

		if (meta.getAnnotationAttributes(EnablePublisher.class.getName()) != null) {
			new PublisherRegistrar().
					registerBeanDefinitions(meta, registry);
		}
	}

	/**
	 * Register {@link IntegrationConfigurationBeanFactoryPostProcessor}
	 * to process the external Integration infrastructure.
	 */
	private void registerIntegrationConfigurationBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(
				IntegrationContextUtils.INTEGRATION_CONFIGURATION_POST_PROCESSOR_BEAN_NAME)) {

			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationConfigurationBeanFactoryPostProcessor.class)
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_CONFIGURATION_POST_PROCESSOR_BEAN_NAME,
					postProcessorBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register the default datatype channel MessageConverter.
	 * @param registry the registry.
	 */
	private void registerDefaultDatatypeChannelMessageConverter(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry
					.isBeanNameInUse(IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder converterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(DefaultDatatypeChannelMessageConverter.class);
			registry.registerBeanDefinition(
					IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
					converterBuilder.getBeanDefinition());
		}

	}

	/**
	 * Register the default {@link CompositeMessageConverter} for argument resolvers
	 * during handler method invocation.
	 * @param registry the registry.
	 */
	private void registerArgumentResolverMessageConverter(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME)) {
			BeanDefinitionBuilder converterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurableCompositeMessageConverter.class);
			registry.registerBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
					converterBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register the default {@link HandlerMethodArgumentResolversHolder} for handler
	 * method invocation.
	 * @param registry the registry.
	 */
	private void registerArgumentResolvers(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVERS_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVERS_BEAN_NAME,
					internalArgumentResolversBuilder(false));
		}
	}

	/**
	 * Register the default {@link HandlerMethodArgumentResolversHolder} for handler
	 * method invocation for lists.
	 * @param registry the registry.
	 */
	private void registerListCapableArgumentResolvers(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.LIST_ARGUMENT_RESOLVERS_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.LIST_ARGUMENT_RESOLVERS_BEAN_NAME,
					internalArgumentResolversBuilder(true));
		}
	}

	private BeanDefinition internalArgumentResolversBuilder(boolean listCapable) {
		ManagedList<BeanDefinition> resolvers = new ManagedList<>();
		resolvers.add(new RootBeanDefinition(PayloadExpressionArgumentResolver.class));
		resolvers.add(new RootBeanDefinition(PayloadsArgumentResolver.class));
		resolvers.add(new RootBeanDefinition(MapArgumentResolver.class));

		if (listCapable) {
			resolvers.add(
					BeanDefinitionBuilder.genericBeanDefinition(CollectionArgumentResolver.class)
							.addConstructorArgValue(true)
							.getBeanDefinition());
		}

		return BeanDefinitionBuilder.genericBeanDefinition(HandlerMethodArgumentResolversHolder.class)
				.addConstructorArgValue(resolvers)
				.getBeanDefinition();
	}

	private void registerMessageBuilderFactory(BeanDefinitionRegistry registry) {
		boolean alreadyRegistered = false;
		if (registry instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) registry)
					.containsBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		}
		else {
			alreadyRegistered = registry
					.isBeanNameInUse(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder mbfBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(DefaultMessageBuilderFactory.class)
					.addPropertyValue("readOnlyHeaders",
							IntegrationProperties.getExpressionFor(IntegrationProperties.READ_ONLY_HEADERS));
			registry.registerBeanDefinition(
					IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME,
					mbfBuilder.getBeanDefinition());
		}

	}

}
