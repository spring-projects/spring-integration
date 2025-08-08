/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.http.config;

import java.util.Arrays;

import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * The {@link WebFluxConfigurer} implementation for CORS mapping on the Integration Graph Controller.
 *
 * @author Artem Bilan
 *
 * @since 5.3.9
 *
 * @see EnableIntegrationGraphController
 */
final class WebFluxIntegrationGraphCorsConfigurer implements WebFluxConfigurer {

	private final String path;

	private final String[] allowedOrigins;

	WebFluxIntegrationGraphCorsConfigurer(String path, String[] allowedOrigins) {
		this.path = path;
		this.allowedOrigins = Arrays.copyOf(allowedOrigins, allowedOrigins.length);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping(this.path).allowedOrigins(this.allowedOrigins).allowedMethods("GET");
	}

}
