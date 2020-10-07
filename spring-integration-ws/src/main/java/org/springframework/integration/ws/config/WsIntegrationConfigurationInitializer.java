/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * @author Artem Bilan
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
						BeanDefinitionBuilder.genericBeanDefinition(MessageEndpointAdapter.class);
				requestMappingBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
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
