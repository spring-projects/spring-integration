/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.http.outbound;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to a {@link RestTemplate} instance. If the 'expectReply' flag is set to true (the default)
 * then a reply Message will be generated from the HTTP response. If that response contains
 * a body, it will be used as the reply Message's payload. Otherwise the reply Message's
 * payload will contain the response status as an instance of the {@link HttpStatus} enum.
 * When there is a response body, the {@link HttpStatus} enum instance will instead be
 * copied to the MessageHeaders of the reply. In both cases, the response headers will
 * be mapped to the reply Message's headers by this handler's {@link HeaderMapper} instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Wallace Wadge
 * @author Shiliang Li
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {
	private final RestTemplate restTemplate;

	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(URI uri) {
		this(new ValueExpression<URI>(uri));
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 *
	 * @param uriExpression The URI expression.
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate
	 * @param uri The URI.
	 * @param restTemplate The rest template.
	 */
	public HttpRequestExecutingMessageHandler(String uri, RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link BeanFactory}.
	 * @param restTemplate The rest template.
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression, RestTemplate restTemplate) {
		super(uriExpression);
		this.restTemplate = (restTemplate == null ? new RestTemplate() : restTemplate);
	}

	@Override
	public String getComponentType() {
		return (this.getExpectReply() ? "http:outbound-gateway" : "http:outbound-channel-adapter");
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 *
	 * @param errorHandler The error handler.
	 *
	 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	@Override
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.restTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 *
	 * @param messageConverters The message converters.
	 *
	 * @see RestTemplate#setMessageConverters(java.util.List)
	 */
	@Override
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.restTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 *
	 * @param requestFactory The request factory.
	 *
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
	}

	@Override
	protected Object exchange(URI uri, HttpMethod httpMethod, HttpEntity<?> httpRequest, Object expectedResponseType) {
		ResponseEntity<?> httpResponse;
		if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
			httpResponse = this.restTemplate.exchange(uri, httpMethod, httpRequest, (ParameterizedTypeReference<?>) expectedResponseType);
		}
		else {
			httpResponse = this.restTemplate.exchange(uri, httpMethod, httpRequest, (Class<?>) expectedResponseType);
		}
		return getReply(httpResponse);
	}
}
