/*
 * Copyright 2026-present the original author or authors.
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.RetryingTest;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.util.StopWatch;

/**
 * @author Jiandong Ma
 */
class MessagingAnnotationsConfigurationTests {

	@ParameterizedTest
	@ValueSource(classes = {DefaultConfig.class, EnableAnnotationsConfig.class})
	void testEnableAnnotations(Class<?> configClass) {
		try (var context = new AnnotationConfigApplicationContext(configClass)) {

			var integrationProperties = IntegrationContextUtils.getIntegrationProperties(context);

			Assertions.assertThat(integrationProperties)
					.extracting(IntegrationProperties::isEnableAnnotations)
					.isEqualTo(true);

			String serviceActivator = ServiceActivatorAnnotatedBean.class.getName() + ".handlePayload.serviceActivator";
			String serviceActivatorHandler = serviceActivator + ".handler";

			Assertions.assertThat(context.containsBean(serviceActivator)).isTrue();
			Assertions.assertThat(context.containsBean(serviceActivatorHandler)).isTrue();
		}
	}

	@Test
	void testDisableAnnotations() {
		try (var context = new AnnotationConfigApplicationContext(DisableAnnotationsConfig.class)) {

			var integrationProperties = IntegrationContextUtils.getIntegrationProperties(context);

			Assertions.assertThat(integrationProperties)
					.extracting(IntegrationProperties::isEnableAnnotations)
					.isEqualTo(false);

			String serviceActivator = ServiceActivatorAnnotatedBean.class.getName() + ".handlePayload.serviceActivator";
			String serviceActivatorHandler = serviceActivator + ".handler";

			Assertions.assertThat(context.containsBean(serviceActivator)).isFalse();
			Assertions.assertThat(context.containsBean(serviceActivatorHandler)).isFalse();
		}
	}

	@RetryingTest(10)
	void testEnableVersusDisablePerformance() {
		// JVM warmup
		new AnnotationConfigApplicationContext(EnableAnnotationsConfig.class);
		new AnnotationConfigApplicationContext(DisableAnnotationsConfig.class);

		StopWatch stopWatch = new StopWatch();

		stopWatch.start();
		new AnnotationConfigApplicationContext(EnableAnnotationsConfig.class);
		stopWatch.stop();
		long enableAnnotationsStartupTime = stopWatch.lastTaskInfo().getTimeMillis(); // ~= 40ms

		stopWatch.start();
		new AnnotationConfigApplicationContext(DisableAnnotationsConfig.class);
		stopWatch.stop();
		long disableAnnotationsStartupTime = stopWatch.lastTaskInfo().getTimeMillis(); // ~= 25ms

		Assertions.assertThat(enableAnnotationsStartupTime).isGreaterThan(disableAnnotationsStartupTime);
	}

	@EnableIntegration
	@Import({ChannelConfig.class, ServiceActivatorAnnotatedBean.class})
	static class DefaultConfig {

	}

	@EnableIntegration
	@Import({ChannelConfig.class, ServiceActivatorAnnotatedBean.class})
	static class EnableAnnotationsConfig {

		@Bean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
		IntegrationProperties integrationProperties() {
			var integrationProperties = IntegrationProperties.DEFAULT_INSTANCE;
			integrationProperties.setEnableAnnotations(true);
			return integrationProperties;
		}

	}

	@EnableIntegration
	@Import({ChannelConfig.class, ServiceActivatorAnnotatedBean.class})
	static class DisableAnnotationsConfig {

		@Bean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
		IntegrationProperties integrationProperties() {
			var integrationProperties = IntegrationProperties.DEFAULT_INSTANCE;
			integrationProperties.setEnableAnnotations(false);
			return integrationProperties;
		}

	}

	@Configuration
	static class ChannelConfig {

		@Bean
		DirectChannel inputChannel() {
			return new DirectChannel();
		}

	}

	static class ServiceActivatorAnnotatedBean {

		@ServiceActivator(inputChannel = "inputChannel")
		void handlePayload(String payload) {

		}

	}

}
