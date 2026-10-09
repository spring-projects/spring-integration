/*
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.http.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	private static final Log LOGGER = LogFactory.getLog(ControlBusControllerConfiguration.class);

	@Bean
	@Nullable
	ControlBusController controlBusController(ControlBusCommandRegistry controlBusCommandRegistry,
			FormattingConversionService conversionService) {

		if (!HttpContextUtils.WEB_MVC_PRESENT && !HttpContextUtils.WEB_FLUX_PRESENT) {
			LOGGER.warn("The 'IntegrationGraphController' isn't registered with the application context because" +
					" there is no 'spring-mvc' or 'spring-webflux' in the classpath.");
			return null;
		}

		controlBusCommandRegistry.setEagerInitialization(true);

		return new ControlBusController(controlBusCommandRegistry, conversionService);
	}

}
