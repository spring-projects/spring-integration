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

import java.beans.Introspector;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RetryingTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StopWatch;

/**
 * @author Jiandong Ma
 */
class IntegrationRegistrarTests {

	@Test
	void testDefaultEnableMessagingAnnotationsProcessing() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {

			String serviceActivator = ServiceActivatorComponent.class.getName() + ".handlePayload.serviceActivator";
			String serviceActivatorHandler = serviceActivator + ".handler";

			assertContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertContainsBean(context, serviceActivator);
			assertContainsBean(context, serviceActivatorHandler);

		}
	}

	@Test
	void testManualEnableMessagingAnnotationsProcessing() {
		try (var context = createApplicationContext(true)) {

			String serviceActivator = ServiceActivatorComponent.class.getName() + ".handlePayload.serviceActivator";
			String serviceActivatorHandler = serviceActivator + ".handler";

			assertContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertContainsBean(context, serviceActivator);
			assertContainsBean(context, serviceActivatorHandler);

		}
	}

	@Test
	void testDisableMessagingAnnotationsProcessing() {
		try (var context = createApplicationContext(false)) {

			String serviceActivator = ServiceActivatorComponent.class.getName() + ".handlePayload.serviceActivator";
			String serviceActivatorHandler = serviceActivator + ".handler";

			assertDoesNotContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertDoesNotContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertDoesNotContainsBean(context, serviceActivator);
			assertDoesNotContainsBean(context, serviceActivatorHandler);

		}
	}

	@RetryingTest(10)
	void testEnableVersusDisablePerformance() {
		// JVM warmup
		try (var ctx1 = createApplicationContext(true); var ctx2 = createApplicationContext(false)) {
			ctx1.getApplicationName();
			ctx2.getApplicationName();
		}

		StopWatch stopWatch = new StopWatch();

		long enabledStartupTime;
		long disabledStartupTime;

		stopWatch.start();
		try (var context = createApplicationContext(true)) {
			context.getApplicationName();

			stopWatch.stop();

			enabledStartupTime = stopWatch.lastTaskInfo().getTimeMillis();
		}

		stopWatch.start();
		try (var context = createApplicationContext(false)) {
			context.getApplicationName();

			stopWatch.stop();

			disabledStartupTime = stopWatch.lastTaskInfo().getTimeMillis();
		}

		Assertions.assertThat(enabledStartupTime).isGreaterThan(disabledStartupTime);
	}

	@Configuration
	@EnableIntegration
	@Import(ServiceActivatorComponent.class)
	static class Config {

		@Bean
		DirectChannel inputChannel() {
			return new DirectChannel();
		}

	}

	static class ServiceActivatorComponent {

		@ServiceActivator(inputChannel = "inputChannel")
		void handlePayload(String payload) {

		}

	}

	static AnnotationConfigApplicationContext createApplicationContext(boolean enableAnnotationsProcessing) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(),
				IntegrationRegistrar.ENV_ENABLE_MESSAGING_ANNOTATIONS_PROCESSING + "=" + enableAnnotationsProcessing);

		context.register(Config.class);
		context.refresh();
		return context;
	}

	static void assertContainsBean(ApplicationContext context, String beanName) {
		Assertions.assertThat(context.containsBean(beanName)).isTrue();
	}

	static void assertDoesNotContainsBean(ApplicationContext context, String beanName) {
		Assertions.assertThat(context.containsBean(beanName)).isFalse();
	}

}
