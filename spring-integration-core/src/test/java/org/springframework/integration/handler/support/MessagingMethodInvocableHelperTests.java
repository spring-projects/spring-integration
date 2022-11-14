/*
 * Copyright 2019-2022 the original author or authors.
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
