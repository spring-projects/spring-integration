/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.camel.dsl;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class CamelDslTests {

	@Autowired
	@Qualifier("camelFlow.input")
	MessageChannel input;

	@Test
	void sendAndReceiveCamelRoute() {
		String result = new MessagingTemplate().convertSendAndReceive(this.input, "apache camel", String.class);
		assertThat(result).isEqualTo("___APACHE CAMEL___");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		CamelContext springCamelContext() {
			return new SpringCamelContext();
		}

		@EventListener(ContextRefreshedEvent.class)
		public void simpleRoute() throws Exception {
			RouteBuilder.addRoutes(springCamelContext(),
					rb -> rb.from("direct:simple").bean("camelDslTests.Config", "transformPayload"));
		}

		@Bean
		IntegrationFlow camelFlow() {
			return f -> f
					.handle(Camel.gateway().endpointUri("direct:simple"))
					.handle(Camel.route(this::camelRoute));
		}

		private void camelRoute(RouteBuilder routeBuilder) {
			routeBuilder.from("direct:inbound").transform(routeBuilder.simple("${body.toUpperCase()}"));
		}

		public String transformPayload(String payload) {
			return "___" + payload + "___";
		}

	}

}
