/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.dsl.routers;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class NoFallbackAllowedTests {

	@Test
	void noStackOverflow(@Autowired Config config) {
		config.flow()
				.getInputChannel()
				.send(new GenericMessage<>("foo", Collections.singletonMap("whereTo", "flow.input")));
		assertThat(config.queue().receive(0)).isNotNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow flow() {
			return f -> f.route("headers.whereTo", r -> r.defaultOutputChannel(queue()));
		}

		@Bean
		public QueueChannel queue() {
			return new QueueChannel();
		}

	}

}
