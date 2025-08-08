/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support.management;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurerTests {

	@Test
	public void testDefaults() {
		DirectChannel channel = new DirectChannel();
		AbstractMessageHandler handler = new RecipientListRouter();
		AbstractMessageSource<?> source = new AbstractMessageSource<Object>() {

			@Override
			public String getComponentType() {
				return null;
			}

			@Override
			protected Object doReceive() {
				return null;
			}
		};
		assertThat(channel.isLoggingEnabled()).isTrue();
		assertThat(handler.isLoggingEnabled()).isTrue();
		assertThat(source.isLoggingEnabled()).isTrue();
		ApplicationContext ctx = mock(ApplicationContext.class);
		Map<String, IntegrationManagement> beans = new HashMap<>();
		beans.put("foo", channel);
		beans.put("bar", handler);
		beans.put("baz", source);
		when(ctx.getBeansOfType(IntegrationManagement.class)).thenReturn(beans);
		IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
		configurer.setBeanName(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
		configurer.setApplicationContext(ctx);
		configurer.setDefaultLoggingEnabled(false);
		configurer.afterSingletonsInstantiated();
		assertThat(channel.isLoggingEnabled()).isFalse();
		assertThat(handler.isLoggingEnabled()).isFalse();
		assertThat(source.isLoggingEnabled()).isFalse();
	}

	@Test
	public void testEmptyAnnotation() {
		try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigEmptyAnnotation.class)) {
			AbstractMessageChannel channel = ctx.getBean("channel", AbstractMessageChannel.class);
			assertThat(channel.isLoggingEnabled()).isTrue();
			channel = ctx.getBean("loggingOffChannel", AbstractMessageChannel.class);
			assertThat(channel.isLoggingEnabled()).isFalse();
		}
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class ConfigEmptyAnnotation {

		@Bean
		public MessageChannel channel() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel loggingOffChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.setLoggingEnabled(false);
			return directChannel;
		}

	}

}
