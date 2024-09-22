/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.websocket.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The WebSocket Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 * @author Ngoc Nhan
 *
 * @since 4.1
 */
public class WebSocketIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final Log LOGGER = LogFactory.getLog(WebSocketIntegrationConfigurationInitializer.class);

	private static final boolean SERVLET_PRESENT = ClassUtils.isPresent("jakarta.servlet.Servlet", null);

	private static final String WEB_SOCKET_HANDLER_MAPPING_BEAN_NAME = "integrationWebSocketHandlerMapping";

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
			registerEnableWebSocketIfNecessary(beanDefinitionRegistry);
		}
		else {
			LOGGER.warn("'DelegatingWebSocketConfiguration' isn't registered because 'beanFactory'" +
					" isn't an instance of `BeanDefinitionRegistry`.");
		}
	}

	/**
	 * Register a {@link WebSocketHandlerMappingFactoryBean} which could also be overridden
	 * by the user by simply using {@link org.springframework.web.socket.config.annotation.EnableWebSocket}
	 * <p>
	 * In addition, checks if the {@code jakarta.servlet.Servlet} class is present on the classpath.
	 * When Spring Integration WebSocket support is used only as a WebSocket client,
	 * there is no reason to use and register the Spring WebSocket server components.
	 * <p>
	 * Note, there is no XML equivalent for
	 * the {@link org.springframework.web.socket.config.annotation .EnableWebSocket}
	 * in the Spring WebSocket. therefore this registration can be used to process
	 * {@link WebSocketConfigurer} implementations without annotation configuration.
	 * From other side it can be used to replace
	 * {@link org.springframework.web.socket.config.annotation.EnableWebSocket} in the Spring Integration
	 * applications when {@link org.springframework.integration.config.EnableIntegration} is in use.
	 */
	private void registerEnableWebSocketIfNecessary(BeanDefinitionRegistry registry) {
		if (SERVLET_PRESENT) {
			if (!registry.containsBeanDefinition("defaultSockJsTaskScheduler")) {

				BeanDefinitionBuilder beanDefinitionBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class)
								.addPropertyValue("threadNamePrefix", "SockJS-")
								.addPropertyValue("poolSize", Runtime.getRuntime().availableProcessors())
								.addPropertyValue("removeOnCancelPolicy", true);
				registry.registerBeanDefinition("defaultSockJsTaskScheduler", beanDefinitionBuilder.getBeanDefinition());
			}

			if (!registry.containsBeanDefinition(DelegatingWebSocketConfiguration.class.getName()) &&
					!registry.containsBeanDefinition(WEB_SOCKET_HANDLER_MAPPING_BEAN_NAME)) {

				registry.registerBeanDefinition("integrationServletWebSocketHandlerRegistry",
						new RootBeanDefinition(IntegrationServletWebSocketHandlerRegistry.class));

				BeanDefinitionBuilder beanDefinitionBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(IntegrationDynamicWebSocketHandlerMapping.class)
								.addPropertyValue("patternParser", new PathPatternParser())
								.addPropertyValue("order", 0);
				BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinitionBuilder.getBeanDefinition(), registry);

				BeanDefinitionBuilder enableWebSocketBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(WebSocketHandlerMappingFactoryBean.class)
								.addPropertyReference("registry", "integrationServletWebSocketHandlerRegistry")
								.addPropertyReference("sockJsTaskScheduler", "defaultSockJsTaskScheduler")
								.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

				registry.registerBeanDefinition(WEB_SOCKET_HANDLER_MAPPING_BEAN_NAME,
						enableWebSocketBuilder.getBeanDefinition());
			}
		}
	}

	static class WebSocketHandlerMappingFactoryBean extends AbstractFactoryBean<HandlerMapping> {

		private IntegrationServletWebSocketHandlerRegistry registry;

		private ThreadPoolTaskScheduler sockJsTaskScheduler;

		public void setRegistry(IntegrationServletWebSocketHandlerRegistry registry) {
			this.registry = registry;
		}

		public void setSockJsTaskScheduler(ThreadPoolTaskScheduler sockJsTaskScheduler) {
			this.sockJsTaskScheduler = sockJsTaskScheduler;
		}

		@Override
		protected HandlerMapping createInstance() {
			BeanFactory beanFactory = getBeanFactory();
			if (beanFactory != null) {
				((ListableBeanFactory) beanFactory)
						.getBeansOfType(WebSocketConfigurer.class)
						.values()
						.forEach(configurer -> configurer.registerWebSocketHandlers(this.registry));
			}
			this.registry.setTaskScheduler(this.sockJsTaskScheduler);
			return this.registry.getHandlerMapping();
		}

		@Override
		public Class<?> getObjectType() {
			return HandlerMapping.class;
		}

	}

}
