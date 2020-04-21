/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.support.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
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

/**
 * @author Gary Russell
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
		channel.setCountsEnabled(true);
		channel.setStatsEnabled(true);
		ApplicationContext ctx = mock(ApplicationContext.class);
		Map<String, IntegrationManagement> beans = new HashMap<String, IntegrationManagement>();
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
		assertThat(channel.isCountsEnabled()).isTrue();
		assertThat(channel.isStatsEnabled()).isTrue();
	}

	@Test
	public void testEmptyAnnotation() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ConfigEmptyAnnotation.class);
		AbstractMessageChannel channel = ctx.getBean("channel", AbstractMessageChannel.class);
		assertThat(channel.isCountsEnabled()).isTrue();
		assertThat(channel.isStatsEnabled()).isTrue();
		channel = ctx.getBean("loggingOffChannel", AbstractMessageChannel.class);
		assertThat(channel.isLoggingEnabled()).isFalse();
		ctx.close();
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
