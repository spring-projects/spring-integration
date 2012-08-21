/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.http.inbound;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Internal class for mapping web requests onto specific {@link HttpRequestHandlingEndpointSupport}.
 *
 * @author Artem Bilan
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @since 3.0
 */
class RequestMapping {

	private String[] pathPatterns;

	private HttpMethod[] methods = new HttpMethod[]{HttpMethod.GET, HttpMethod.POST};

	private String[] params;

	private String[] headers;

	private String[] consumes;

	private String[] produces;

	public void setPathPatterns(String... pathPatterns) {
		this.pathPatterns = pathPatterns;
	}

	public String[] getPathPatterns() {
		return pathPatterns;
	}

	public void setMethods(HttpMethod... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported methods is required");
		this.methods = supportedMethods;
	}

	public HttpMethod[] getMethods() {
		return methods;
	}

	public void setParams(String... params) {
		this.params = params;
	}

	public String[] getParams() {
		return params;
	}

	public void setHeaders(String... headers) {
		this.headers = headers;
	}

	public String[] getHeaders() {
		return headers;
	}

	public void setConsumes(String... consumes) {
		this.consumes = consumes;
	}

	public String[] getConsumes() {
		return consumes;
	}

	public void setProduces(String... produces) {
		this.produces = produces;
	}

	public String[] getProduces() {
		return produces;
	}

	RequestMethod[] getRequestMethods() {
		RequestMethod[] requestMethods = new RequestMethod[this.methods.length];
		for (int i = 0; i < this.methods.length; i++) {
			requestMethods[i] = RequestMethod.valueOf(this.methods[i].name());
		}
		return requestMethods;
	}
}
