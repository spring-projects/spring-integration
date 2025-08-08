/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
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
