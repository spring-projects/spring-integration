/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.http.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.integration.http.management.ControlBusController;
import org.springframework.integration.support.management.ControlBusCommandRegistry;

/**
 * Registers the {@link ControlBusController} bean.
 * <p>
 * Also calls {@link ControlBusCommandRegistry#setEagerInitialization(boolean)} with {@code true}
 * to load all the available commands in the application context.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ControlBusControllerConfiguration {

	private static final Log LOGGER = LogFactory.getLog(IntegrationGraphControllerRegistrar.class);

	@Bean
	ControlBusController controlBusController(ControlBusCommandRegistry controlBusCommandRegistry,
			ObjectProvider<FormattingConversionService> conversionService) {

		if (!HttpContextUtils.WEB_MVC_PRESENT && !HttpContextUtils.WEB_FLUX_PRESENT) {
			LOGGER.warn("The 'IntegrationGraphController' isn't registered with the application context because" +
					" there is no 'spring-mvc' or 'spring-webflux' in the classpath.");
			return null;
		}

		controlBusCommandRegistry.setEagerInitialization(true);

		return new ControlBusController(controlBusCommandRegistry, conversionService.getIfUnique());
	}

}
