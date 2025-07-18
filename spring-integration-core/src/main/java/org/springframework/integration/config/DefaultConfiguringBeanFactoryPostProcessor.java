/*
 * Copyright 2002-present the original author or authors.
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.ChannelUtils;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.support.IntegrationMessageHandlerMethodFactory;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.integration.support.management.ControlBusCommandRegistry;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanDefinitionRegistryPostProcessor} implementation that registers bean definitions
 * for many infrastructure components with their default configurations.
 * All of them can be overridden using particular bean names.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Michael Wiles
 * @author Pierre Lakreb
 * @author Chris Bono
 *
 * @see IntegrationContextUtils
 */
public class DefaultConfiguringBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private static final LogAccessor LOGGER = new LogAccessor(DefaultConfiguringBeanFactoryPostProcessor.class);

	private static final Set<Integer> REGISTRIES_PROCESSED = new HashSet<>();

	private static final @Nullable Class<?> XPATH_CLASS;

	private static final boolean JSON_PATH_PRESENT = ClassUtils.isPresent("com.jayway.jsonpath.JsonPath", null);

	static {
		Class<?> xpathClass = null;
		try {
			xpathClass = ClassUtils.forName(IntegrationContextUtils.BASE_PACKAGE + ".xml.xpath.XPathUtils",
					ClassUtils.getDefaultClassLoader());
		}
		catch (@SuppressWarnings("unused") ClassNotFoundException e) {
			LOGGER.debug("SpEL function '#xpath' isn't registered: " +
					"there is no spring-integration-xml.jar on the classpath.");
		}
		finally {
			XPATH_CLASS = xpathClass;
		}
	}

	@SuppressWarnings("NullAway.Init")
	private ConfigurableListableBeanFactory beanFactory;

	@SuppressWarnings("NullAway.Init")
	private BeanDefinitionRegistry registry;

	DefaultConfiguringBeanFactoryPostProcessor() {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		this.registry = registry;
		this.beanFactory = (ConfigurableListableBeanFactory) registry;

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
		registerIntegrationConfigurationReport();
		registerControlBusCommandRegistry();
		registerApplicationRunningController();
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	private void registerBeanFactoryChannelResolver() {
		if (!this.beanFactory.containsBeanDefinition(ChannelResolverUtils.CHANNEL_RESOLVER_BEAN_NAME)) {
			this.registry.registerBeanDefinition(ChannelResolverUtils.CHANNEL_RESOLVER_BEAN_NAME,
					new RootBeanDefinition(BeanFactoryChannelResolver.class));
		}
	}

	private void registerMessagePublishingErrorHandler() {
		if (!this.beanFactory.containsBeanDefinition(ChannelUtils.MESSAGE_PUBLISHING_ERROR_HANDLER_BEAN_NAME)) {
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
				if (beanFactoryToUse instanceof ConfigurableListableBeanFactory listable &&
						listable.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {

					nullChannelDefinition = listable.getBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
				}
				if (beanFactoryToUse instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
					beanFactoryToUse = hierarchicalBeanFactory.getParentBeanFactory();
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
	 * Also, a {@link IntegrationContextUtils#ERROR_LOGGER_BEAN_NAME} is registered as a subscriber for this
	 * error channel.
	 */
	private void registerErrorChannel() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)) {
			LOGGER.info(() -> "No bean named '" + IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME +
					"' has been explicitly defined. " +
					"Therefore, a default PublishSubscribeChannel will be created.");
			this.registry.registerBeanDefinition(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME,
					BeanDefinitionBuilder.rootBeanDefinition(PublishSubscribeChannel.class)
							.addConstructorArgValue(IntegrationProperties.getExpressionFor(
									IntegrationProperties.ERROR_CHANNEL_REQUIRE_SUBSCRIBERS))
							.addPropertyValue("ignoreFailures", IntegrationProperties.getExpressionFor(
									IntegrationProperties.ERROR_CHANNEL_IGNORE_FAILURES))
							.getBeanDefinition());

			String errorLoggerBeanName =
					IntegrationContextUtils.ERROR_LOGGER_BEAN_NAME + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
			this.registry.registerBeanDefinition(errorLoggerBeanName,
					BeanDefinitionBuilder.genericBeanDefinition(LoggingHandler.class)
							.addConstructorArgValue(LoggingHandler.Level.ERROR)
							.addPropertyValue(IntegrationNamespaceUtils.ORDER, Ordered.LOWEST_PRECEDENCE - 100)
							.getBeanDefinition());

			BeanDefinitionBuilder loggingEndpointBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class)
							.addPropertyValue("inputChannelName", IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
							.addPropertyReference("handler", errorLoggerBeanName);

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
			BeanDefinitionBuilder integrationEvaluationContextBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationEvaluationContextFactoryBean.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
					integrationEvaluationContextBuilder.getBeanDefinition());
		}

		if (!this.registry.containsBeanDefinition(
				IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME)) {

			BeanDefinitionBuilder integrationEvaluationContextBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationSimpleEvaluationContextFactoryBean.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME,
					integrationEvaluationContextBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register an {@link IdGeneratorConfigurer} in the application context.
	 */
	private void registerIdGeneratorConfigurer() {
		Class<IdGeneratorConfigurer> clazz = IdGeneratorConfigurer.class;
		String className = clazz.getName();
		String[] definitionNames = this.registry.getBeanDefinitionNames();
		for (String definitionName : definitionNames) {
			BeanDefinition definition = this.registry.getBeanDefinition(definitionName);
			if (className.equals(definition.getBeanClassName())) {
				LOGGER.info(() -> className + " is already registered and will be used");
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
			LOGGER.info(() -> "No bean named '" + IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME +
					"' has been explicitly defined. " +
					"Therefore, a default ThreadPoolTaskScheduler will be created.");
			BeanDefinition scheduler =
					BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class)
							.addPropertyValue("poolSize", IntegrationProperties.getExpressionFor(
									IntegrationProperties.TASK_SCHEDULER_POOL_SIZE))
							.addPropertyValue("threadNamePrefix", "task-scheduler-")
							.addPropertyValue("rejectedExecutionHandler", new CallerRunsPolicy())
							.addPropertyValue("phase", SmartLifecycle.DEFAULT_PHASE / 2)
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
			BeanDefinition userProperties =
					BeanDefinitionBuilder.genericBeanDefinition(PropertiesFactoryBean.class)
							.addPropertyValue("locations", "classpath*:META-INF/spring.integration.properties")
							.getBeanDefinition();

			BeanDefinition integrationProperties =
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationProperties.class)
							.setFactoryMethod("parse")
							.addConstructorArgValue(userProperties)
							.getBeanDefinition();

			this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME,
					integrationProperties);
		}
	}

	/**
	 * Register {@code jsonPath} and {@code xpath} SpEL-function beans, if necessary.
	 */
	private void registerBuiltInBeans() {
		int registryId = System.identityHashCode(this.registry);
		jsonPath(registryId);
		xpath(registryId);
		REGISTRIES_PROCESSED.add(registryId);
	}

	private void jsonPath(int registryId) throws LinkageError {
		String jsonPathBeanName = "jsonPath";
		if (JSON_PATH_PRESENT) {
			if (!this.beanFactory.containsBean(jsonPathBeanName) && !REGISTRIES_PROCESSED.contains(registryId)) {
				IntegrationConfigUtils.registerSpelFunctionBean(this.registry, jsonPathBeanName,
						JsonPathUtils.class, "evaluate");
			}
		}
		else {
			LOGGER.debug("The '#jsonPath' SpEL function cannot be registered: " +
					"there is no jayway json-path.jar on the classpath.");
		}
	}

	private void xpath(int registryId) throws LinkageError {
		String xpathBeanName = "xpath";
		if (XPATH_CLASS != null
				&& !this.beanFactory.containsBean(xpathBeanName)
				&& !REGISTRIES_PROCESSED.contains(registryId)) {

			IntegrationConfigUtils.registerSpelFunctionBean(this.registry, xpathBeanName, XPATH_CLASS, "evaluate");
		}
	}

	/**
	 * Register a {@link SmartLifecycleRoleController} if necessary.
	 */
	private void registerRoleController() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER)) {
			this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER,
					new RootBeanDefinition(SmartLifecycleRoleController.class));
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
					IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME, mbfBuilder.getBeanDefinition());
		}
	}

	/**
	 * Register a {@link DefaultHeaderChannelRegistry} if necessary.
	 */
	private void registerHeaderChannelRegistry() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME)) {
			LOGGER.info(() -> "No bean named '" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME +
					"' has been explicitly defined. Therefore, a default DefaultHeaderChannelRegistry will be created.");

			this.registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
					new RootBeanDefinition(DefaultHeaderChannelRegistry.class));
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

			this.registry.registerBeanDefinition(
					IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
					new RootBeanDefinition(DefaultDatatypeChannelMessageConverter.class));
		}
	}

	/**
	 * Register the default {@link ConfigurableCompositeMessageConverter} for argument
	 * resolvers during handler method invocation.
	 */
	private void registerArgumentResolverMessageConverter() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME)) {
			this.registry.registerBeanDefinition(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
					new RootBeanDefinition(ConfigurableCompositeMessageConverter.class));
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

	private void registerIntegrationConfigurationReport() {
		this.registry.registerBeanDefinition(Introspector.decapitalize(IntegrationConfigurationReport.class.getName()),
				BeanDefinitionBuilder.genericBeanDefinition(IntegrationConfigurationReport.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
						.getBeanDefinition());
	}

	private void registerControlBusCommandRegistry() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.CONTROL_BUS_COMMAND_REGISTRY_BEAN_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(ControlBusCommandRegistry.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			this.registry.registerBeanDefinition(IntegrationContextUtils.CONTROL_BUS_COMMAND_REGISTRY_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	private void registerApplicationRunningController() {
		if (!this.beanFactory.containsBean(IntegrationContextUtils.APPLICATION_RUNNING_CONTROLLER_BEAN_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(ApplicationRunningController.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			this.registry.registerBeanDefinition(IntegrationContextUtils.APPLICATION_RUNNING_CONTROLLER_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	private static BeanDefinitionBuilder createMessageHandlerMethodFactoryBeanDefinition(boolean listCapable) {
		return BeanDefinitionBuilder.genericBeanDefinition(IntegrationMessageHandlerMethodFactory.class)
				.addConstructorArgValue(listCapable)
				.addPropertyReference("messageConverter",
						IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME);
	}

}
