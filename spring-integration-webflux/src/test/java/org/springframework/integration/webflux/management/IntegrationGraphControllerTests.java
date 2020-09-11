/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.integration.webflux.management;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.config.EnableIntegrationGraphController;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * @author Artem Bilan
 *
 * @since 5.0.2
 */
@SpringJUnitWebConfig
@TestPropertySource(properties = "spring.application.name:testApplication")
@DirtiesContext
public class IntegrationGraphControllerTests {

	@Autowired
	private WebApplicationContext wac;

	@Test
	public void testIntegrationGraphGet() {
		WebTestClient webTestClient =
				WebTestClient.bindToApplicationContext(this.wac)
						.build();

		webTestClient.get().uri("/testIntegration")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.nodes..name").isArray()
				.jsonPath("$.contentDescriptor.name").isEqualTo("testApplication")
				.jsonPath("$.links").exists();
	}

	@Configuration
	@EnableWebFlux
	@EnableIntegration
	@EnableIntegrationGraphController(path = "/testIntegration")
	public static class ContextConfiguration {

	}

}
