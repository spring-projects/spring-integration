/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.http.inbound;

import java.util.Arrays;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The mapping to permit cross origin requests (CORS) for {@link HttpRequestHandlingEndpointSupport}.
 * Provides direct mapping in terms of functionality compared to
 * {@link org.springframework.web.bind.annotation.CrossOrigin}.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 *
 * @see org.springframework.web.bind.annotation.CrossOrigin
 * @see IntegrationRequestMappingHandlerMapping
 */
public class CrossOrigin {

	private String[] origin = {"*"};

	private String[] allowedHeaders = {"*"};

	private String[] exposedHeaders = {};

	private RequestMethod[] method = {};

	private Boolean allowCredentials = true;

	private long maxAge = 1800; // NOSONAR magic number

	public void setOrigin(String... origin) {
		this.origin = Arrays.copyOf(origin, origin.length);
	}

	public String[] getOrigin() {
		return this.origin; // NOSONAR - expose internals
	}

	public void setAllowedHeaders(String... allowedHeaders) {
		this.allowedHeaders = Arrays.copyOf(allowedHeaders, allowedHeaders.length);
	}

	public String[] getAllowedHeaders() {
		return this.allowedHeaders; // NOSONAR - expose internals
	}

	public void setExposedHeaders(String... exposedHeaders) {
		this.exposedHeaders = Arrays.copyOf(exposedHeaders, exposedHeaders.length);
	}

	public String[] getExposedHeaders() {
		return this.exposedHeaders; // NOSONAR - expose internals
	}

	public void setMethod(RequestMethod... method) {
		this.method = Arrays.copyOf(method, method.length);
	}

	public RequestMethod[] getMethod() {
		return this.method; // NOSONAR - expose internals
	}

	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public long getMaxAge() {
		return this.maxAge;
	}

}
