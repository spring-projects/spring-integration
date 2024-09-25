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

package org.springframework.integration.endpoint;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.endpoint.management.IntegrationKeepAlive;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 6.4
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationKeepAliveTests {

	@Test
	void keepAliveIsActive(@Autowired IntegrationKeepAlive integrationKeepAlive) {
		assertThat(integrationKeepAlive.isRunning()).isTrue();
		Thread keepAliveThread = TestUtils.getPropertyValue(integrationKeepAlive, "keepAliveThread", Thread.class);
		assertThat(keepAliveThread.isAlive()).isTrue();
		integrationKeepAlive.stop();
		await().untilAsserted(() -> assertThat(keepAliveThread.isAlive()).isFalse());
		integrationKeepAlive.start();
	}

	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

	}

	@Nested
	@ContextConfiguration(classes = WithPollingEndpoint.WithPollingEndpointConfig.class)
	class WithPollingEndpoint {

		@Test
		void keepAliveNotActive(@Autowired IntegrationKeepAlive integrationKeepAlive) {
			assertThat(integrationKeepAlive.isRunning()).isFalse();
		}

		@Configuration
		static class WithPollingEndpointConfig {

			@Bean
			AbstractPollingEndpoint mockPollingEndpoint() {
				return mock();
			}

		}

	}

	@Nested
	@ContextConfiguration(classes = WithDaemonTaskScheduler.WithDaemonTaskSchedulerConfig.class)
	class WithDaemonTaskScheduler {

		@Test
		void keepAliveActive(@Autowired IntegrationKeepAlive integrationKeepAlive) {
			assertThat(integrationKeepAlive.isRunning()).isTrue();
		}

		@Configuration
		static class WithDaemonTaskSchedulerConfig {

			@Bean
			AbstractPollingEndpoint mockPollingEndpoint() {
				return mock();
			}

			@Bean
			String daemonSetter(ThreadPoolTaskScheduler taskScheduler) {
				taskScheduler.setDaemon(true);
				return null;
			}

		}

	}

	@Nested
	@ContextConfiguration(classes = WithGlobalProperty.WithGlobalPropertyConfig.class)
	class WithGlobalProperty {

		@Test
		void keepAliveNotActive(@Autowired IntegrationKeepAlive integrationKeepAlive) {
			assertThat(integrationKeepAlive.isRunning()).isFalse();
		}

		@Configuration
		static class WithGlobalPropertyConfig {

			@Bean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
			static IntegrationProperties integrationProperties() {
				IntegrationProperties integrationProperties = new IntegrationProperties();
				integrationProperties.setKeepAlive(false);
				return integrationProperties;
			}

		}

	}

}
