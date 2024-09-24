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
