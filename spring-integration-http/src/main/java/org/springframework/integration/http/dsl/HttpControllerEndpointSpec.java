/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.dsl;

import org.springframework.integration.http.inbound.HttpRequestHandlingController;

/**
 * The {@link BaseHttpInboundEndpointSpec} implementation for the {@link HttpRequestHandlingController}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see HttpRequestHandlingController
 */
public class HttpControllerEndpointSpec
		extends BaseHttpInboundEndpointSpec<HttpControllerEndpointSpec, HttpRequestHandlingController> {

	protected HttpControllerEndpointSpec(HttpRequestHandlingController controller, String... path) {
		super(controller, path);
	}

	/**
	 * Specify the key to be used when adding the reply Message or payload to the core map
	 * (will be payload only unless the value
	 * of {@link HttpRequestHandlingController#setExtractReplyPayload(boolean)} is <code>false</code>).
	 * The default key is {@code reply}.
	 * @param replyKey The reply key.
	 * @return the spec
	 * @see HttpRequestHandlingController#setReplyKey(String)
	 */
	public HttpControllerEndpointSpec replyKey(String replyKey) {
		this.target.setReplyKey(replyKey);
		return this;
	}

	/**
	 * The key used to expose {@link org.springframework.validation.Errors} in the core,
	 * in the case that message handling fails.
	 * Defaults to {@code errors}.
	 * @param errorsKey The key value to set.
	 * @return the spec
	 * @see HttpRequestHandlingController#setErrorsKey(String)
	 */
	public HttpControllerEndpointSpec errorsKey(String errorsKey) {
		this.target.setErrorsKey(errorsKey);
		return this;
	}

	/**
	 * The error code to use to signal an error in the message handling.
	 * @param errorCode The error code to set.
	 * @return the spec
	 * @see HttpRequestHandlingController#setErrorCode(String)
	 */
	public HttpControllerEndpointSpec errorCode(String errorCode) {
		this.target.setErrorCode(errorCode);
		return this;
	}

}
