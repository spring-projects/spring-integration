/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.channel.ChannelUtils;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.handler.support.MapArgumentResolver;
import org.springframework.integration.handler.support.PayloadExpressionArgumentResolver;
import org.springframework.integration.handler.support.PayloadsArgumentResolver;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.NullAwarePayloadArgumentResolver;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanFactoryPostProcessor} implementation that registers bean definitions
 * for many infrastructure components with their default configurations.
 * All of them can be overridden using particular bean names.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Michael Wiles
 * @author Pierre Lakreb
 *
 * @see IntegrationContextUtils
 */
class DefaultConfiguringBeanFactoryPostProcessor
		implements BeanFactoryPostProcessor, BeanClassLoaderAware, SmartInitializingSingleton {

	private static final Log LOGGER = LogFactory.getLog(DefaultConfiguringBeanFactoryPostProcessor.class);

	private static final IntegrationConverterInitializer INTEGRATION_CONVERTER_INITIALIZER =
			new IntegrationConverterInitializer();

	private static final Set<Integer> REGISTRIES_PROCESSED = new HashSet<>();

	private ClassLoader classLoader;

	private ConfigurableListableBeanFactory beanFactory;

	private BeanDefinitionRegistry registry;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			this.beanFactory = beanFactory;
			this.registry = (BeanDefinitionRegistry) beanFactory;

			registerBeanFactoryChannelResolver();
			registerMessagePublishingErrorHandler();
			registerNullChannel();
			registerErrorChannel();
			registerIntegrationEvaluationContext();
			registerTaskScheduler();
			registerIdGeneratorConfigurer();
			registerIntegrationProperties();
			registerBuiltInBeans();
			registerRoleController();
			registerMessageBuilderFactory();
			registerHeaderChannelRegistry();
			registerGlobalChannelInterceptorProcessor();
			registerDefaultDatatypeChannelMessageConverter();
			registerArgumentResolverMessageConverter();
			registerMessageHandlerMethodFactory();
			registerListMessageHandlerMethodFactory();
		}
		else if (LOGGER.isWarnEnabled()) {
			LOGGER.warn("BeanFactory is not a BeanDefinitionRegistry. " +
					"The default Spring Integration infrastructure beans are not going to be registered");
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (LOGGER.isDebugEnabled()) {
			Properties integrationProperties = IntegrationContextUtils.getIntegrationProperties(this.beanFactory);

			StringWriter writer = new StringWriter();
			integrationProperties.list(new PrintWriter(writer));
			StringBuffer propertiesBuffer = writer.getBuffer()
					.delete(0, "-- listing properties --".length());
			LOGGER.debug("\nSpring Integration global properties:\n" + propertiesBuffer);
		}
	}

	private void registerBeanFactoryChannelResolver() {
		if (!this.beanFactory.containsBeanDefinition(ChannelResolverUtils.CHANNEL_RESOLVER_BEAN_NAME)) {
			this.registry.registerBeanDefinition(ChannelResolverUtils.CHANNEL_RESOLVER_BEAN_NAME,
					new RootBeanDefinition(BeanFactoryChannelResolver.class));
		}
	}

	private void registerMessagePublishingErrorHandler() {
		if (!this.beanFactory.containsBeanDefinition(
				ChannelUtils.MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME)) {
			this.registry.registerBeanDefinition(ChannelUtils.MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME,
					new RootBeanDefinition(MessagePublishingErrorHandler.class));
		}
	}

	/**
	 * Register a null channel in the application context.
	 * The bean name is defined by the constant {@link IntegrationContextUtils#NULL_CHANNEL_BEAN_NAME}.
	 */
	private void registerNullChannel() {
		if (this.beanFactory.containsBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {
			BeanDefinition nullChannelDefinition = null;
			BeanFactory beanFactoryToUse = this.beanFactory;
			do {
				if (beanFactoryToUse instanceof ConfigurableListableBeanFactory) {
					ConfigurableListableBeanFactory listable = (ConfigurableListableBeanFactory) beanFactoryToUse;
					if (listable.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {
						nullChannelDefinition = listable
								.getBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
					}
				}
				if (beanFactoryToUse instanceof HierarchicalBeanFactory) {
					beanFactoryToUse = ((HierarchicalBeanFactory) beanFactoryToUse).getParentBeanFactory();
				}
			}
			while (nullChannelDefinition == null);

			if (!NullChannel.class.getName().equals(nullChannelDefinition.getBeanClassName())) {
				throw new IllegalStateException("The bean name '" + IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME
						+ "' is reserved.");
			}
		}
		else {
			this.registry.registerBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME,
					new RootBeanDefinition(NullChannel.class));
		}
	}

	/**
	 * Register an error channel in the application context.
	 * The bean name is defined by the constant {@link IntegrationContextUtils#ERROR_CHANNEL_BEAN_NAME}.
	 * Also a {@link IntegrationContextUtils#ERROR_LOGGER_BEAN_NAME} is registered as a subscriber for this
	 * error channel.
	 */
	private void registerErrorChannel() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("No bean named '" + IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME +
						"' has been explicitly defined. " +
						"Therefore, a default PublishSubscribeChannel will be created.");
			}
			this.registry.registerBeanDefinition(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME,
					BeanDefinitionBuilder.rootBeanDefinition(PublishSubscribeChannel.class)
							.addConstructorArgValue(IntegrationProperties.getExpressionFor(
									IntegrationProperties.ERROR_CHANNEL_REQUIRE_SUBSCRIBERS))
							.addPropertyValue("ignoreFailures",
									IntegrationProperties.getExpressionFor(
											IntegrationProperties.ERROR_CHANNEL_IGNORE_FAILURES))
							.getBeanDefinition());

			BeanDefinition loggingHandler =
					BeanDefinitionBuilder.genericBeanDefinition(LoggingHandler.class)
							.addConstructorArgValue("ERROR")
							.getBeanDefinition();

			String errorLoggerBeanName =
					IntegrationContextUtils.ERROR_LOGGER_BEAN_NAME + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
			this.registry.registerBeanDefinition(errorLoggerBeanName, loggingHandler);

			BeanDefinitionBuilder loggingEndpointBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class)
							.addPropertyReference("handler", errorLoggerBeanName)
							.addPropertyValue("inputChannelName", IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);

			BeanComponentDefinition componentDefinition =
					new BeanComponentDefinition(loggingEndpointBuilder.getBeanDefinition(),
							IntegrationContextUtils.ERROR_LOGGER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(componentDefinition, this.registry);
		}
	}

	/**
	 * Register {@link IntegrationEvaluationContextFactoryBean}
	 * and {@link IntegrationSimpleEvaluationContextFactoryBean} beans, if necessary.
	 */
	private void registerIntegrationEvaluationContext() {
		if (!this.registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)) {
			BeanDefinitionBuilder integrationEvaluationContextBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationEvaluationContextFactoryBean.class);
			integrationEvaluationContextBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			BeanDefinitionHolder integrationEvaluationContextHolder =
					new BeanDefinitionHolder(integrationEvaluationContextBuilder.getBeanDefinition(),
							IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME);

			BeanDefinitionReaderUtils.registerBeanDefinition(integrationEvaluationContextHolder, this.registry);
		}

		if (!this.registry.containsBeanDefinition(
				IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME)) {

			BeanDefinitionBuilder integrationEvaluationContextBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationSimpleEvaluationContextFactoryBean.class);
			integrationEvaluationContextBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			BeanDefinitionHolder integrationEvaluationContextHolder =
					new BeanDefinitionHolder(integrationEvaluationContextBuilder.getBeanDefinition(),
							IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME);

			BeanDefinitionReaderUtils.registerBeanDefinition(integrationEvaluationContextHolder, this.registry);
		}
	}

	/**
	 * Register an {@link IdGeneratorConfigurer} in the application context.
	 */
	private void registerIdGeneratorConfigurer() {
		Class<?> clazz = IdGeneratorConfigurer.class;
		String className = clazz.getName();
		String[] definitionNames = this.registry.getBeanDefinitionNames();
		for (String definitionName : definitionNames) {
			BeanDefinition definition = this.registry.getBeanDefinition(definitionName);
			if (className.equals(definition.getBeanClassName())) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(className + " is already registered and will be used");
				}
				return;
			}
		}
		RootBeanDefinition beanDefinition = new RootBeanDefinition(clazz);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, this.registry);
	}

	/**
	 * Register a {@link ThreadPoolTaskScheduler}  bean in the application context.
	 */
	private void registerTaskScheduler() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("No bean named '" + IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME +
						"' has been explicitly defined. " +
						"Therefore, a default ThreadPoolTaskScheduler will be created.");
			}
			BeanDefinition scheduler = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class)
					.addPropertyValue("poolSize", IntegrationProperties
							.getExpressionFor(IntegrationProperties.TASK_SCHEDULER_POOL_SIZE))
					.addPropertyValue("threadNamePrefix", "task-scheduler-")
					.addPropertyValue("rejectedExecutionHandler", new CallerRunsPolicy())
					.addPropertyReference("errorHandler",
							ChannelUtils.MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME)
					.getBeanDefinition();

			this.registry.registerBeanDefinition(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, scheduler);
		}
	}

	/**
	 * Register an {@code integrationGlobalProperties} bean if necessary.
	 */
	private void registerIntegrationProperties() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)) {
			ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(this.classLoader);
			try {
				Resource[] defaultResources =
						resourceResolver.getResources("classpath*:META-INF/spring.integration.default.properties");
				Resource[] userResources =
						resourceResolver.getResources("classpath*:META-INF/spring.integration.properties");

				List<Resource> resources = new LinkedList<>(Arrays.asList(defaultResources));
				resources.addAll(Arrays.asList(userResources));

				BeanDefinitionBuilder integrationPropertiesBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(PropertiesFactoryBean.class)
						.addPropertyValue("locations", resources);

				this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME,
						integrationPropertiesBuilder.getBeanDefinition());
			}
			catch (IOException e) {
				LOGGER.warn("Cannot load 'spring.integration.properties' Resources.", e);
			}
		}
	}

	/**
	 * Register {@code jsonPath} and {@code xpath} SpEL-function beans, if necessary.
	 */
	private void registerBuiltInBeans() {
		int registryId = System.identityHashCode(this.registry);
		jsonPath(registryId);
		xpath(registryId);
		jsonNodeToString(registryId);
		REGISTRIES_PROCESSED.add(registryId);
	}

	private void jsonPath(int registryId) throws LinkageError {
		String jsonPathBeanName = "jsonPath";
		if (!this.beanFactory.containsBean(jsonPathBeanName) && !REGISTRIES_PROCESSED.contains(registryId)) {
			Class<?> jsonPathClass = null;
			try {
				jsonPathClass = ClassUtils.forName("com.jayway.jsonpath.JsonPath", this.classLoader);
			}
			catch (@SuppressWarnings("unused") ClassNotFoundException e) {
				LOGGER.debug("The '#jsonPath' SpEL function cannot be registered: " +
						"there is no jayway json-path.jar on the classpath.");
			}

			if (jsonPathClass != null) {
				try {
					ClassUtils.forName("com.jayway.jsonpath.Predicate", this.classLoader);
				}
				catch (ClassNotFoundException e) {
					jsonPathClass = null;
					LOGGER.warn("The '#jsonPath' SpEL function cannot be registered. " +
							"An old json-path.jar version is detected in the classpath." +
							"At least 2.4.0 is required; see version information at: " +
							"https://github.com/jayway/JsonPath/releases", e);

				}
			}

			if (jsonPathClass != null) {
				IntegrationConfigUtils.registerSpelFunctionBean(this.registry, jsonPathBeanName,
						IntegrationConfigUtils.BASE_PACKAGE + ".json.JsonPathUtils", "evaluate");
			}
		}
	}

	private void xpath(int registryId) throws LinkageError {
		String xpathBeanName = "xpath";
		if (!this.beanFactory.containsBean(xpathBeanName) && !REGISTRIES_PROCESSED.contains(registryId)) {
			Class<?> xpathClass = null;
			try {
				xpathClass = ClassUtils.forName(IntegrationConfigUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils",
						this.classLoader);
			}
			catch (@SuppressWarnings("unused") ClassNotFoundException e) {
				LOGGER.debug("SpEL function '#xpath' isn't registered: " +
						"there is no spring-integration-xml.jar on the classpath.");
			}

			if (xpathClass != null) {
				IntegrationConfigUtils.registerSpelFunctionBean(this.registry, xpathBeanName,
						IntegrationConfigUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils", "evaluate");
			}
		}
	}

	private void jsonNodeToString(int registryId) {
		if (!this.beanFactory.containsBean(
				IntegrationContextUtils.JSON_NODE_WRAPPER_TO_JSON_NODE_CONVERTER) &&
				!REGISTRIES_PROCESSED.contains(registryId) && JacksonPresent.isJackson2Present()) {

			this.registry.registerBeanDefinition(
					IntegrationContextUtils.JSON_NODE_WRAPPER_TO_JSON_NODE_CONVERTER,
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationConfigUtils.BASE_PACKAGE +
							".json.JsonNodeWrapperToJsonNodeConverter")
							.getBeanDefinition());
			INTEGRATION_CONVERTER_INITIALIZER.registerConverter(this.registry,
					new RuntimeBeanReference(
							IntegrationContextUtils.JSON_NODE_WRAPPER_TO_JSON_NODE_CONVERTER));
		}
	}

	/**
	 * Register a {@link SmartLifecycleRoleController} if necessary.
	 */
	private void registerRoleController() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(SmartLifecycleRoleController.class);
			builder.addConstructorArgValue(new ManagedList<String>());
			builder.addConstructorArgValue(new ManagedList<Lifecycle>());
			this.registry.registerBeanDefinition(
					IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER, builder.getBeanDefinition());
		}
	}

	/**
	 * Register a {@link DefaultMessageBuilderFactory} if necessary.
	 */
	private void registerMessageBuilderFactory() {
		if (!this.beanFactory.containsBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME)) {
			BeanDefinitionBuilder mbfBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(DefaultMessageBuilderFactory.class)
					.addPropertyValue("readOnlyHeaders",
							IntegrationProperties.getExpressionFor(IntegrationProperties.READ_ONLY_HEADERS));
			this.registry.registerBeanDefinition(
					IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME,
					mbfBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register a {@link DefaultHeaderChannelRegistry} if necessary.
	 */
	private void registerHeaderChannelRegistry() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("No bean named '" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME +
						"' has been explicitly defined. " +
						"Therefore, a default DefaultHeaderChannelRegistry will be created.");
			}
			BeanDefinitionBuilder schedulerBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultHeaderChannelRegistry.class);
			BeanDefinitionHolder replyChannelRegistryComponent = new BeanDefinitionHolder(
					schedulerBuilder.getBeanDefinition(),
					IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(replyChannelRegistryComponent, this.registry);
		}
	}

	/**
	 * Register a {@link GlobalChannelInterceptorProcessor} if necessary.
	 */
	private void registerGlobalChannelInterceptorProcessor() {
		if (!this.registry.containsBeanDefinition(
				IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_PROCESSOR_BEAN_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorProcessor.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			this.registry.registerBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_PROCESSOR_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	/**
	 * Register a {@link DefaultDatatypeChannelMessageConverter} bean if necessary.
	 */
	private void registerDefaultDatatypeChannelMessageConverter() {
		if (!this.beanFactory.containsBean(
				IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME)) {

			BeanDefinitionBuilder converterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(DefaultDatatypeChannelMessageConverter.class);
			this.registry.registerBeanDefinition(
					IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
					converterBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register the default {@link ConfigurableCompositeMessageConverter} for argument
	 * resolvers during handler method invocation.
	 */
	private void registerArgumentResolverMessageConverter() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME)) {
			BeanDefinitionBuilder converterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurableCompositeMessageConverter.class);
			this.registry.registerBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
					converterBuilder.getBeanDefinition());
		}
	}

	private void registerMessageHandlerMethodFactory() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.MESSAGE_HANDLER_FACTORY_BEAN_NAME)) {
			BeanDefinitionBuilder messageHandlerMethodFactoryBuilder =
					createMessageHandlerMethodFactoryBeanDefinition(false);
			this.registry.registerBeanDefinition(IntegrationContextUtils.MESSAGE_HANDLER_FACTORY_BEAN_NAME,
					messageHandlerMethodFactoryBuilder.getBeanDefinition());
		}
	}

	private void registerListMessageHandlerMethodFactory() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.LIST_MESSAGE_HANDLER_FACTORY_BEAN_NAME)) {
			BeanDefinitionBuilder messageHandlerMethodFactoryBuilder =
					createMessageHandlerMethodFactoryBeanDefinition(true);
			this.registry.registerBeanDefinition(IntegrationContextUtils.LIST_MESSAGE_HANDLER_FACTORY_BEAN_NAME,
					messageHandlerMethodFactoryBuilder.getBeanDefinition());
		}
	}

	private BeanDefinitionBuilder createMessageHandlerMethodFactoryBeanDefinition(boolean listCapable) {
		return BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageHandlerMethodFactory.class)
				.addPropertyReference("messageConverter",
						IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME)
				.addPropertyValue("customArgumentResolvers", buildArgumentResolvers(listCapable));
	}

	private ManagedList<BeanDefinition> buildArgumentResolvers(boolean listCapable) {
		ManagedList<BeanDefinition> resolvers = new ManagedList<>();
		resolvers.add(new RootBeanDefinition(PayloadExpressionArgumentResolver.class));
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(NullAwarePayloadArgumentResolver.class);
		builder.addConstructorArgReference(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME);
		// TODO Validator ?
		resolvers.add(builder.getBeanDefinition());
		resolvers.add(new RootBeanDefinition(PayloadsArgumentResolver.class));

		if (listCapable) {
			resolvers.add(
					BeanDefinitionBuilder.genericBeanDefinition(CollectionArgumentResolver.class)
							.addConstructorArgValue(true)
							.getBeanDefinition());
		}

		resolvers.add(new RootBeanDefinition(MapArgumentResolver.class));
		return resolvers;
	}

}
