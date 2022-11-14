/*
 * Copyright 2015-2022 the original author or authors.
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
