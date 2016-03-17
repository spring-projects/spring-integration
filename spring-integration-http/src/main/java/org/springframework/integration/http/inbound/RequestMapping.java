/*
 * Copyright 2013-2016 the original author or authors.
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

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Class for mapping web requests onto specific {@link HttpRequestHandlingEndpointSupport}.
 * Provides direct mapping in terms of functionality compared to
 * {@link org.springframework.web.bind.annotation.RequestMapping}.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see IntegrationRequestMappingHandlerMapping
 */
public class RequestMapping {

	private String[] pathPatterns;

	private HttpMethod[] methods = new HttpMethod[]{HttpMethod.GET, HttpMethod.POST};

	private String[] params = new String[0];

	private String[] headers = new String[0];

	private String[] consumes = new String[0];

	private String[] produces = new String[0];

	public void setPathPatterns(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "at least one path pattern is required");
		this.pathPatterns = pathPatterns;
	}

	public String[] getPathPatterns() {
		return this.pathPatterns;
	}

	public void setMethods(HttpMethod... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported methods is required");
		this.methods = supportedMethods;
	}

	public HttpMethod[] getMethods() {
		return this.methods;
	}

	public void setParams(String... params) {
		Assert.notEmpty(params, "at least one param is required");
		this.params = params;
	}

	public String[] getParams() {
		return this.params;
	}

	public void setHeaders(String... headers) {
		Assert.notEmpty(headers, "at least one header is required");
		this.headers = headers;
	}

	public String[] getHeaders() {
		return this.headers;
	}

	public void setConsumes(String... consumes) {
		Assert.notEmpty(consumes, "at least one consume value is required");
		this.consumes = consumes;
	}

	public String[] getConsumes() {
		return this.consumes;
	}

	public void setProduces(String... produces) {
		Assert.notEmpty(produces, "at least one produce value is required");
		this.produces = produces;
	}

	public String[] getProduces() {
		return this.produces;
	}

	public RequestMethod[] getRequestMethods() {
		RequestMethod[] requestMethods = new RequestMethod[this.methods.length];
		for (int i = 0; i < this.methods.length; i++) {
			requestMethods[i] = RequestMethod.valueOf(this.methods[i].name());
		}
		return requestMethods;
	}

}
