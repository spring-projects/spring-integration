/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A {@link BeanFactoryPostProcessor} implementation that provides default beans for the error handling and task
 * scheduling if those beans have not already been explicitly defined within the registry. It also registers a single
 * null channel with the bean name "nullChannel".
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
class DefaultConfiguringBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private static final String ERROR_LOGGER_BEAN_NAME = "_org.springframework.integration.errorLogger";


	private final Log logger = LogFactory.getLog(this.getClass());


	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			this.registerNullChannel(registry);
			if (!beanFactory.containsBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)) {
				this.registerErrorChannel(registry);
			}
			if (!beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)) {
				this.registerTaskScheduler(registry);
			}
			this.registerIdGeneratorConfigurer(registry);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("BeanFactory is not a BeanDefinitionRegistry. The default '"
					+ IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME + "' and '"
					+ IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME + "' cannot be configured."
					+ " Also, any custom IdGenerator implementation configured in this BeanFactory"
					+ " will not be recognized.");
		}
	}

	private void registerInfrastructureBean(BeanDefinitionRegistry registry, String className) {
		String[] definitionNames = registry.getBeanDefinitionNames();
		for (String definitionName : definitionNames) {
			BeanDefinition definition = registry.getBeanDefinition(definitionName);
			if (className.equals(definition.getBeanClassName())) {
				if (logger.isInfoEnabled()) {
					logger.info(className + " is already registered and will be used");
				}
				return;
			}
		}
		RootBeanDefinition beanDefinition = new RootBeanDefinition(className);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
	}

	private void registerIdGeneratorConfigurer(BeanDefinitionRegistry registry) {
		registerInfrastructureBean(registry, "org.springframework.integration.config.IdGeneratorConfigurer");
	}

	/**
	 * Register a null channel in the given BeanDefinitionRegistry. The bean name is defined by the constant
	 * {@link IntegrationContextUtils#NULL_CHANNEL_BEAN_NAME}.
	 */
	private void registerNullChannel(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {
			BeanDefinition nullChannelDefinition = registry.getBeanDefinition(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
			if (NullChannel.class.getName().equals(nullChannelDefinition.getBeanClassName())) {
				return;
			}
			else {
				throw new IllegalStateException("The bean name '" + IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME
						+ "' is reserved.");
			}
		}
		else {
			RootBeanDefinition nullChannelDef = new RootBeanDefinition();
			nullChannelDef.setBeanClassName(IntegrationConfigUtils.BASE_PACKAGE + ".channel.NullChannel");
			BeanDefinitionHolder nullChannelHolder = new BeanDefinitionHolder(nullChannelDef,
					IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(nullChannelHolder, registry);
		}
	}

	/**
	 * Register an error channel in the given BeanDefinitionRegistry.
	 */
	private void registerErrorChannel(BeanDefinitionRegistry registry) {
		if (logger.isInfoEnabled()) {
			logger.info("No bean named '" + IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME +
					"' has been explicitly defined. Therefore, a default PublishSubscribeChannel will be created.");
		}
		registry.registerBeanDefinition(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME,
				new RootBeanDefinition(PublishSubscribeChannel.class));

		BeanDefinitionBuilder loggingHandlerBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(LoggingHandler.class).addConstructorArgValue("ERROR");

		BeanDefinitionBuilder loggingEndpointBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class)
						.addPropertyValue("handler", loggingHandlerBuilder.getBeanDefinition())
						.addPropertyValue("inputChannelName", IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);

		BeanComponentDefinition componentDefinition =
				new BeanComponentDefinition(loggingEndpointBuilder.getBeanDefinition(), ERROR_LOGGER_BEAN_NAME);
		BeanDefinitionReaderUtils.registerBeanDefinition(componentDefinition, registry);
	}

	/**
	 * Register a TaskScheduler in the given BeanDefinitionRegistry.
	 */
	private void registerTaskScheduler(BeanDefinitionRegistry registry) {
		if (logger.isInfoEnabled()) {
			logger.info("No bean named '" + IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME +
					"' has been explicitly defined. Therefore, a default ThreadPoolTaskScheduler will be created.");
		}
		BeanDefinition scheduler = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class)
				.addPropertyValue("poolSize", IntegrationProperties.getExpressionFor(IntegrationProperties.TASK_SCHEDULER_POOL_SIZE))
				.addPropertyValue("threadNamePrefix", "task-scheduler-")
				.addPropertyValue("rejectedExecutionHandler", new CallerRunsPolicy())
				.addPropertyValue("errorHandler", new RootBeanDefinition(MessagePublishingErrorHandler.class))
				.getBeanDefinition();

		registry.registerBeanDefinition(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, scheduler);
	}

}
