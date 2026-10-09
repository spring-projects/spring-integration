/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.config;

import java.beans.Introspector;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * @author Jiandong Ma
 */
class IntegrationRegistrarTests {

	@Test
	void testDefaultEnableMessagingAnnotationsProcessing() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {

			assertContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertContainsBean(context, "inputChannel");
			assertContainsBean(context, "customMessageHandler");
			assertContainsBean(context, "customMessageHandler.serviceActivator");
			assertContainsBean(context, "customMessageHandler.serviceActivator.handler");
		}
	}

	@Test
	void testManualEnableMessagingAnnotationsProcessing() {
		try (var context = createApplicationContext(true)) {

			assertContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertContainsBean(context, "inputChannel");
			assertContainsBean(context, "customMessageHandler");
			assertContainsBean(context, "customMessageHandler.serviceActivator");
			assertContainsBean(context, "customMessageHandler.serviceActivator.handler");
		}
	}

	@Test
	void testDisableMessagingAnnotationsProcessing() {
		try (var context = createApplicationContext(false)) {

			assertDoesNotContainsBean(context, IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME);
			assertDoesNotContainsBean(context, Introspector.decapitalize(MessagingAnnotationBeanPostProcessor.class.getName()));

			assertDoesNotContainsBean(context, "inputChannel");
			assertContainsBean(context, "customMessageHandler");
			assertDoesNotContainsBean(context, "customMessageHandler.serviceActivator");
			assertDoesNotContainsBean(context, "customMessageHandler.serviceActivator.handler");

		}
	}

	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		MessageHandler customMessageHandler() {
			return message -> {

			};
		}

	}

	static AnnotationConfigApplicationContext createApplicationContext(boolean enableAnnotationsProcessing) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context.getEnvironment(),
				"spring.integration.annotations.enable=" + enableAnnotationsProcessing);

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
