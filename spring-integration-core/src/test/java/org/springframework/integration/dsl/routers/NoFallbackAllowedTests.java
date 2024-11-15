/*
 * Copyright 2019-2024 the original author or authors.
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
