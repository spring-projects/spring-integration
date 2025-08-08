/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.ws.server.EndpointAdapter;
import org.springframework.ws.server.endpoint.adapter.MessageEndpointAdapter;

/**
 * The {@link IntegrationConfigurationInitializer} implementation for the WebService module.
 * Registers {@link MessageEndpointAdapter} bean, because the usage of
 * {@link org.springframework.ws.config.annotation.EnableWs} switches off the registration for the
 * {@link MessageEndpointAdapter} as one of the default strategies.
 * The {@link org.springframework.ws.config.annotation.EnableWs}
 * registers only the {@link org.springframework.ws.server.endpoint.adapter.DefaultMethodEndpointAdapter},
 * which isn't appropriate for the {@link org.springframework.ws.server.endpoint.MessageEndpoint} implementations.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 4.3
 *
 * @see org.springframework.ws.config.annotation.EnableWs
 * @see org.springframework.ws.server.MessageDispatcher
 */
public class WsIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String MESSAGE_ENDPOINT_ADAPTER_BEAN_NAME = "integrationWsMessageEndpointAdapter";

	private static final Log LOGGER = LogFactory.getLog(WsIntegrationConfigurationInitializer.class);

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			if (beanFactory.getBeanNamesForType(EndpointAdapter.class, false, false).length > 0) {
				BeanDefinitionBuilder requestMappingBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(MessageEndpointAdapter.class)
								.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(MESSAGE_ENDPOINT_ADAPTER_BEAN_NAME,
						requestMappingBuilder.getBeanDefinition());
			}
		}
		else {
			LOGGER.warn("'IntegrationRequestMappingHandlerMapping' isn't registered because 'beanFactory'" +
					" isn't an instance of `BeanDefinitionRegistry`.");
		}
	}

}
