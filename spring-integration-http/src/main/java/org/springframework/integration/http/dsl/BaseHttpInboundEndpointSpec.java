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

import java.util.Arrays;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport;
import org.springframework.web.multipart.MultipartResolver;

/**
 * A base {@link org.springframework.integration.dsl.MessagingGatewaySpec} for the
 * {@link HttpRequestHandlingEndpointSupport} implementations.
 *
 * @param <S> the target {@link BaseHttpInboundEndpointSpec} implementation type.
 * @param <E> the target {@link HttpRequestHandlingEndpointSupport} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class BaseHttpInboundEndpointSpec<S extends BaseHttpInboundEndpointSpec<S, E>,
		E extends HttpRequestHandlingEndpointSupport>
		extends HttpInboundEndpointSupportSpec<S, E> {

	protected BaseHttpInboundEndpointSpec(E endpoint, String... path) {
		super(endpoint, path);
	}

	/**
	 * Set the message body converters to use.
	 * These converters are used to convert from and to HTTP requests and responses.
	 * @param messageConverters The message converters.
	 * @return the current Spec.
	 */
	public S messageConverters(HttpMessageConverter<?>... messageConverters) {
		this.target.setMessageConverters(Arrays.asList(messageConverters));
		return _this();
	}

	/**
	 * Flag which determines if the default converters should be available after custom converters.
	 * @param mergeWithDefaultConverters true to merge, false to replace.
	 * @return the current Spec.
	 */
	public S mergeWithDefaultConverters(boolean mergeWithDefaultConverters) {
		this.target.setMergeWithDefaultConverters(mergeWithDefaultConverters);
		return _this();
	}

	/**
	 * Specify the {@link MultipartResolver} to use when checking requests.
	 * @param multipartResolver The multipart resolver.
	 * @return the current Spec.
	 */
	public S multipartResolver(MultipartResolver multipartResolver) {
		this.target.setMultipartResolver(multipartResolver);
		return _this();
	}

}
