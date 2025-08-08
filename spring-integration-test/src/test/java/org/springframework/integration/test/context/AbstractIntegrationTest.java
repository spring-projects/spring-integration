/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.test.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.ReactiveMessageSourceProducer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Base integration test that specifies a default {@link SpringIntegrationTest}
 * to be inherited by concrete subclasses.
 *
 * @author Chris Bono
 *
 * @since 6.2.10
 */
@SpringJUnitConfig
@SpringIntegrationTest(noAutoStartup = "*")
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
