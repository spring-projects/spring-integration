/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concrete specialization of {@link AbstractIntegrationTest} used to
 * test inherit and override behavior of {@link SpringIntegrationTest}
 * when used with {@link Nested} and {@link NestedTestConfiguration}.
 *
 * @author Chris Bono
 * @author Artem Bilan
 *
 * @since 6.2.10
 */
class NestedSpringIntegrationTestAnnotationTests extends AbstractIntegrationTest {

	@Test
	void annotationDefinedOnParentIsInheritedByDefault(@Autowired AbstractEndpoint mockEndpoint) {
		assertThat(mockEndpoint.isRunning()).isFalse();
	}

	@Nested
	class NestedTestDefaultEnclosingConfiguration {

		@Test
		void annotationDefinedOnParentOfEnclosingIsInheritedByDefault(@Autowired AbstractEndpoint mockEndpoint) {
			assertThat(mockEndpoint.isRunning()).isFalse();
		}

	}

	@Nested
	@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
	class NestedTestWithInheritEnclosingConfiguration {

		@Test
		void annotationDefinedOnParentOfEnclosingIsInherited(@Autowired AbstractEndpoint mockEndpoint) {
			assertThat(mockEndpoint.isRunning()).isFalse();
		}

	}

	@Nested
	@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
	@SpringIntegrationTest(noAutoStartup = "noSuchEndpointWithThisPatternExists")
	class NestedTestWithInheritEnclosingConfigurationButOverrideAnnotation {

		@Test
		void annotationDefinedOnParentOfEnclosingIsOverridden(@Autowired AbstractEndpoint mockEndpoint) {
			assertThat(mockEndpoint.isRunning()).isTrue();
		}

	}

	@Nested
	@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
	@ContextConfiguration(classes = MockEndpointConfig.class)
	class NestedTestWithOverrideEnclosingConfiguration {

		@Test
		void annotationDefinedOnParentOfEnclosingIsIgnored(@Autowired AbstractEndpoint mockEndpoint,
				@Nullable @Autowired MockIntegrationContext mockIntegrationContext) {

			assertThat(mockEndpoint.isRunning()).isTrue();
			assertThat(mockIntegrationContext).isNull();
		}

	}

}
