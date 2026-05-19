/*
 * Copyright 2015-present the original author or authors.
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
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The mapping to permit cross-origin requests (CORS) for {@link HttpRequestHandlingEndpointSupport}.
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

	private String[] origin = {};

	private String[] originPatterns = {};

	private String[] allowedHeaders = {};

	private String[] exposedHeaders = {};

	private RequestMethod[] method = {};

	private Boolean allowCredentials = false;

	private long maxAge = 1800L;

	public void setOrigin(String... origin) {
		this.origin = Arrays.copyOf(origin, origin.length);
	}

	public String[] getOrigin() {
		return this.origin;
	}

	@Nullable
	public List<String> getOriginsList() {
		return ObjectUtils.isEmpty(this.origin) ? null : Arrays.asList(this.origin);
	}

	public void setOriginPatterns(String... originPatterns) {
		this.originPatterns = Arrays.copyOf(originPatterns, originPatterns.length);
	}

	public String[] getOriginPatterns() {
		return this.originPatterns;
	}

	@Nullable
	public List<String> getOriginPatternsList() {
		return ObjectUtils.isEmpty(this.originPatterns) ? null : Arrays.asList(this.originPatterns);
	}

	public void setAllowedHeaders(String... allowedHeaders) {
		this.allowedHeaders = Arrays.copyOf(allowedHeaders, allowedHeaders.length);
	}

	public String[] getAllowedHeaders() {
		return this.allowedHeaders;
	}

	@Nullable
	public List<String> getAllowedHeadersList() {
		return ObjectUtils.isEmpty(this.allowedHeaders) ? null : Arrays.asList(this.allowedHeaders);
	}

	public void setExposedHeaders(String... exposedHeaders) {
		this.exposedHeaders = Arrays.copyOf(exposedHeaders, exposedHeaders.length);
	}

	public String[] getExposedHeaders() {
		return this.exposedHeaders;
	}

	@Nullable
	public List<String> getExposedHeadersList() {
		return ObjectUtils.isEmpty(this.exposedHeaders) ? null : Arrays.asList(this.exposedHeaders);
	}

	public void setMethod(RequestMethod... method) {
		this.method = Arrays.copyOf(method, method.length);
	}

	public RequestMethod[] getMethod() {
		return this.method;
	}

	@Nullable
	public List<RequestMethod> getMethodsList() {
		return ObjectUtils.isEmpty(this.method) ? null : Arrays.asList(this.method);
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
