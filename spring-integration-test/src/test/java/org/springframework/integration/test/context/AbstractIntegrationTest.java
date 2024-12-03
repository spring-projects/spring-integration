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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.ReactiveMessageSourceProducer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Base integration test that specifies a default {@link SpringIntegrationTest}
 * to be inherited by concrete subclasses.
 *
 * @author Chris Bono
 * @author Artem Bilan
 *
 * @since 6.2.10
 */
@SpringJUnitConfig
@SpringIntegrationTest(noAutoStartup = "*")
@DirtiesContext
class AbstractIntegrationTest {

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class MockEndpointConfig {

		@Bean
		AbstractEndpoint mockEndpoint() {
			ReactiveMessageSourceProducer endpoint =
					new ReactiveMessageSourceProducer(() -> new GenericMessage<>("testFromMockEndpoint"));
			endpoint.setOutputChannelName("nullChannel");
			return endpoint;
		}

	}
}
