/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.http.inbound;

import java.util.Arrays;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Class for mapping web requests onto specific {@link HttpRequestHandlingEndpointSupport}.
 * Provides direct mapping in terms of functionality compared to
 * {@link org.springframework.web.bind.annotation.RequestMapping}.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 *
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see IntegrationRequestMappingHandlerMapping
 */
public class RequestMapping {

	private String name = "";

	private String[] pathPatterns;

	private HttpMethod[] methods = {HttpMethod.GET, HttpMethod.POST};

	private String[] params = {};

	private String[] headers = {};

	private String[] consumes = {};

	private String[] produces = {};

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPathPatterns(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "at least one path pattern is required");
		this.pathPatterns = Arrays.copyOf(pathPatterns, pathPatterns.length);
	}

	public String[] getPathPatterns() {
		return this.pathPatterns; // NOSONAR - expose internals
	}

	/**
	 * Configure a set of supported HTTP methods from their string representations.
	 * @param supportedMethods the array of HTTP method names.
	 * @since 6.0
	 */
	public void setMethodNames(String... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported methods is required");
		setMethods(Arrays.stream(supportedMethods).map(HttpMethod::valueOf).toArray(HttpMethod[]::new));
	}

	public void setMethods(HttpMethod... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported methods is required");
		this.methods = Arrays.copyOf(supportedMethods, supportedMethods.length);
	}

	public HttpMethod[] getMethods() {
		return this.methods; // NOSONAR - expose internals
	}

	public void setParams(String... params) {
		Assert.notEmpty(params, "at least one param is required");
		this.params = Arrays.copyOf(params, params.length);
	}

	public String[] getParams() {
		return this.params; // NOSONAR - expose internals
	}

	public void setHeaders(String... headers) {
		Assert.notEmpty(headers, "at least one header is required");
		this.headers = Arrays.copyOf(headers, headers.length);
	}

	public String[] getHeaders() {
		return this.headers; // NOSONAR - expose internals
	}

	public void setConsumes(String... consumes) {
		Assert.notEmpty(consumes, "at least one consume value is required");
		this.consumes = Arrays.copyOf(consumes, consumes.length);
	}

	public String[] getConsumes() {
		return this.consumes; // NOSONAR - expose internals
	}

	public void setProduces(String... produces) {
		Assert.notEmpty(produces, "at least one produce value is required");
		this.produces = Arrays.copyOf(produces, produces.length);
	}

	public String[] getProduces() {
		return this.produces; // NOSONAR - expose internals
	}

	public RequestMethod[] getRequestMethods() {
		RequestMethod[] requestMethods = new RequestMethod[this.methods.length];
		for (int i = 0; i < this.methods.length; i++) {
			requestMethods[i] = RequestMethod.valueOf(this.methods[i].name());
		}
		return requestMethods;
	}

}
