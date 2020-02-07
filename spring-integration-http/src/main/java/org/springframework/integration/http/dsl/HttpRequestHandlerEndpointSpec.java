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
