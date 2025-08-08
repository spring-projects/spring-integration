/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.dsl;

import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;

/**
 * The {@link BaseHttpInboundEndpointSpec} implementation for the {@link HttpRequestHandlingMessagingGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see HttpRequestHandlingMessagingGateway
 */
public class HttpRequestHandlerEndpointSpec
		extends BaseHttpInboundEndpointSpec<HttpRequestHandlerEndpointSpec, HttpRequestHandlingMessagingGateway> {

	protected HttpRequestHandlerEndpointSpec(HttpRequestHandlingMessagingGateway endpoint, String... path) {
		super(endpoint, path);
	}

	/**
	 * Flag to determine if conversion and writing out of message handling exceptions should be attempted.
	 * @param convertExceptions the flag to set
	 * @return the spec
	 */
	public HttpRequestHandlerEndpointSpec convertExceptions(boolean convertExceptions) {
		this.target.setConvertExceptions(convertExceptions);
		return this;
	}

}
