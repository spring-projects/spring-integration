/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.handler.support;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class MessagingMethodInvocableHelperTests {

	@Autowired
	private Config config;

	@Test
	void cachedHandler() {
		this.config.sampleFlow().getInputChannel().send(new GenericMessage<>(Collections.singletonMap("key", "value")));
		Message<?> received = this.config.queue().receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("Hello value World!");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow sampleFlow() {
			return f -> f
					.handle(new MapHandler())
					.handle(new StringHandler())
					.channel(queue());
		}

		@Bean
		public QueueChannel queue() {
			return new QueueChannel();
		}

		public static class MapHandler implements GenericHandler<Map<String, String>> {

			@Override
			public String handle(Map<String, String> mapPayload, MessageHeaders messageHeaders) {
				return "Hello " + mapPayload.get("key");
			}

		}

		public static class StringHandler implements GenericHandler<String> {

			@Override
			public String handle(String stringPayload, MessageHeaders messageHeaders) {
				return stringPayload + " World!";
			}

		}

	}

}
