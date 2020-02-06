/*
 * Copyright 2016-2020 the original author or authors.
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
