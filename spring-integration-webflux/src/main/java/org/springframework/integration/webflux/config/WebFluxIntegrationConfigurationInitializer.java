/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.http.config.HttpContextUtils;
import org.springframework.integration.webflux.inbound.IntegrationHandlerResultHandler;
import org.springframework.integration.webflux.inbound.WebFluxIntegrationRequestMappingHandlerMapping;
import org.springframework.integration.webflux.support.WebFluxContextUtils;

/**
 * The WebFlux Integration infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 5.0
 */
public class WebFluxIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final Log LOGGER = LogFactory.getLog(WebFluxIntegrationConfigurationInitializer.class);

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerReactiveRequestMappingHandlerMappingIfNecessary((BeanDefinitionRegistry) beanFactory);
		}
		else {
			LOGGER.warn("'IntegrationRequestMappingHandlerMapping' isn't registered because 'beanFactory'" +
					" isn't an instance of `BeanDefinitionRegistry`.");
		}
	}

	/**
	 * Registers a {@link WebFluxIntegrationRequestMappingHandlerMapping}
	 * which could also be overridden by the user by simply registering
	 * a {@link WebFluxIntegrationRequestMappingHandlerMapping} {@code <bean>} with 'id'
	 * {@link WebFluxContextUtils#HANDLER_MAPPING_BEAN_NAME}.
	 * <p>
	 * In addition, checks if the {@code org.springframework.web.reactive.result.method.RequestMappingInfo}
	 * class is present on the classpath.
	 * When Spring Integration HTTP is used only as an HTTP client, there is no reason to use and register
	 * the HTTP server components.
	 */
	private void registerReactiveRequestMappingHandlerMappingIfNecessary(BeanDefinitionRegistry registry) {
		if (HttpContextUtils.WEB_FLUX_PRESENT &&
				!registry.containsBeanDefinition(WebFluxContextUtils.HANDLER_MAPPING_BEAN_NAME)) {

			BeanDefinitionBuilder requestMappingBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(WebFluxIntegrationRequestMappingHandlerMapping.class)
							.addPropertyValue("order", 0)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(WebFluxContextUtils.HANDLER_MAPPING_BEAN_NAME,
					requestMappingBuilder.getBeanDefinition());

			BeanDefinitionReaderUtils.registerWithGeneratedName(
					new RootBeanDefinition(IntegrationHandlerResultHandler.class),
					registry);
		}
	}

}
