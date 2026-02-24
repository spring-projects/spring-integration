/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.http.outbound;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * A {@link org.springframework.messaging.MessageHandler}
 * implementation that executes HTTP requests by delegating
 * to a {@link RestTemplate} or {@link RestClient} instance.
 * If the 'expectReply' flag is set to true (the default)
 * then a reply Message will be generated from the HTTP response. If that response contains
 * a body, it will be used as the reply Message's payload. Otherwise the reply Message's
 * payload will contain the response status as an instance of the
 * {@link org.springframework.http.HttpStatus} enum.
 * When there is a response body, the {@link org.springframework.http.HttpStatus} enum
 * instance will instead be
 * copied to the MessageHeaders of the reply. In both cases, the response headers will
 * be mapped to the reply Message's headers by this handler's
 * {@link org.springframework.integration.mapping.HeaderMapper} instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Wallace Wadge
 * @author Shiliang Li
 * @author Arun Sethumadhavan
 *
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {

	private final @Nullable RestTemplate restTemplate;

	private volatile @Nullable RestClient restClient;

	private final RestClient.@Nullable Builder localRestClientBuilder;

	private final boolean restTemplateExplicitlySet;

	private final boolean restClientExplicitlySet;

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(URI uri) {
		this(new ValueExpression<>(uri));
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(String uri) {
		this(uri, (RestClient) null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 * @param uriExpression The URI expression.
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, (RestClient) null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate.
	 * @param uri The URI.
	 * @param restTemplate The rest template.
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public HttpRequestExecutingMessageHandler(String uri, @Nullable RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate.
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link org.springframework.beans.factory.BeanFactory}.
	 * @param restTemplate The rest template.
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public HttpRequestExecutingMessageHandler(Expression uriExpression, @Nullable RestTemplate restTemplate) {
		this(uriExpression, restTemplate, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestClient.
	 * @param uri The URI.
	 * @param restClient The rest client.
	 * @since 7.1
	 */
	public HttpRequestExecutingMessageHandler(String uri, @Nullable RestClient restClient) {
		this(new LiteralExpression(uri), restClient);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestClient.
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link org.springframework.beans.factory.BeanFactory}.
	 * @param restClient The rest client.
	 * @since 7.1
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression, @Nullable RestClient restClient) {
		this(uriExpression, null, restClient);
	}

	private HttpRequestExecutingMessageHandler(Expression uriExpression,
			@Nullable RestTemplate restTemplate, @Nullable RestClient restClient) {

		super(uriExpression);
		Assert.isTrue(restTemplate == null || restClient == null,
				"Only one of 'restTemplate' and 'restClient' may be provided");
		if (restClient != null) {
			this.restTemplate = null;
			this.localRestClientBuilder = null;
			this.restClient = restClient;
			this.restTemplateExplicitlySet = false;
			this.restClientExplicitlySet = true;
		}
		else if (restTemplate != null) {
			this.restTemplate = restTemplate;
			this.localRestClientBuilder = null;
			this.restClient = null;
			this.restTemplateExplicitlySet = true;
			this.restClientExplicitlySet = false;
		}
		else {
			this.restTemplate = null;
			this.localRestClientBuilder =
					RestClient.builder()
							.uriBuilderFactory(this.uriFactory);
			this.restClient = null;
			this.restTemplateExplicitlySet = false;
			this.restClientExplicitlySet = false;
		}
	}

	@Override
	public String getComponentType() {
		return (isExpectReply() ? "http:outbound-gateway" : "http:outbound-channel-adapter");
	}

	private void assertLocalClient(String option) {
		Assert.isTrue(!this.restTemplateExplicitlySet && !this.restClientExplicitlySet, () -> {
			if (this.restTemplateExplicitlySet) {
				return "The option '" + option + "' must be provided on the externally configured RestTemplate: "
						+ this.restTemplate;
			}
			return "The option '" + option + "' must be provided on the externally configured RestClient: "
					+ this.restClient;
		});
	}

	@Override
	protected void doInit() {
		super.doInit();
		RestClient.Builder localRestClientBuilder = this.localRestClientBuilder;
		if (localRestClientBuilder != null) {
			this.restClient = localRestClientBuilder.build();
		}
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 * @param errorHandler The error handler.
	 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		assertLocalClient("errorHandler");
		RestClient.Builder localRestClientBuilder = this.localRestClientBuilder;
		Assert.state(localRestClientBuilder != null, "'localRestClientBuilder' must not be null");
		localRestClientBuilder.defaultStatusHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 * @param messageConverters The message converters.
	 * @see RestTemplate#setMessageConverters(java.util.List)
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		assertLocalClient("messageConverters");
		RestClient.Builder localRestClientBuilder = this.localRestClientBuilder;
		Assert.state(localRestClientBuilder != null, "'localRestClientBuilder' must not be null");
		localRestClientBuilder.configureMessageConverters((builder) ->
				builder.configureMessageConvertersList((converters) -> {
					converters.clear();
					converters.addAll(messageConverters);
				}));
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @param requestFactory The request factory.
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		assertLocalClient("requestFactory");
		RestClient.Builder localRestClientBuilder = this.localRestClientBuilder;
		Assert.state(localRestClientBuilder != null, "'localRestClientBuilder' must not be null");
		localRestClientBuilder.requestFactory(requestFactory);
	}

	@Override
	public void setEncodingMode(DefaultUriBuilderFactory.EncodingMode encodingMode) {
		assertLocalClient("encodingMode on UriTemplateHandler");
		super.setEncodingMode(encodingMode);
	}

	@Override
	protected @Nullable Object exchange(Object uri, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Message<?> requestMessage, Map<String, ?> uriVariables) {

		ResponseEntity<?> httpResponse;
		try {
			RestClient restClientToUse = this.restClient;
			if (restClientToUse != null) {
				httpResponse = exchangeWithRestClient(restClientToUse, uri, httpMethod, httpRequest,
						expectedResponseType, uriVariables);
			}
			else {
				httpResponse = exchangeWithRestTemplate(uri, httpMethod, httpRequest, expectedResponseType,
						uriVariables);
			}

			if (isExpectReply()) {
				return getReply(httpResponse);
			}
			else {
				return null;
			}

		}
		catch (RestClientException e) {
			throw new MessageHandlingException(requestMessage,
					"HTTP request execution failed for URI [" + uri + "] in the [" + this + ']', e);
		}
	}

	private ResponseEntity<?> exchangeWithRestTemplate(Object uri, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Map<String, ?> uriVariables) {

		RestTemplate restTemplate = this.restTemplate;
		Assert.state(restTemplate != null, "'restTemplate' must not be null");

		if (uri instanceof URI uriToUse) {
			if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
				return restTemplate.exchange(uriToUse, httpMethod, httpRequest,
						(ParameterizedTypeReference<?>) expectedResponseType);
			}
			else {
				return restTemplate.exchange(uriToUse, httpMethod, httpRequest, (Class<?>) expectedResponseType);
			}
		}
		else {
			if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
				return restTemplate.exchange((String) uri, httpMethod, httpRequest,
						(ParameterizedTypeReference<?>) expectedResponseType, uriVariables);
			}
			else {
				return restTemplate.exchange((String) uri, httpMethod, httpRequest, (Class<?>) expectedResponseType,
						uriVariables);
			}
		}
	}

	private ResponseEntity<?> exchangeWithRestClient(RestClient restClient, Object uri, HttpMethod httpMethod,
			HttpEntity<?> httpRequest, Object expectedResponseType, Map<String, ?> uriVariables) {

		Assert.notNull(restClient, "'restClient' must not be null");
		Assert.isTrue(uri instanceof URI || uri instanceof String, "'uri' must be a URI or String");

		RestClient.RequestBodyUriSpec uriSpec = restClient.method(httpMethod);
		RestClient.RequestBodySpec requestSpec =
				(uri instanceof URI uriObject)
						? uriSpec.uri(uriObject)
						: uriSpec.uri((String) uri, uriVariables);

		requestSpec.headers((headers) -> headers.putAll(httpRequest.getHeaders()));

		Object body = httpRequest.getBody();
		if (body != null) {
			requestSpec.body(body);
		}

		RestClient.ResponseSpec responseSpec = requestSpec.retrieve();

		if (expectedResponseType == Void.class || expectedResponseType == void.class) {
			return responseSpec.toBodilessEntity();
		}
		else if (expectedResponseType instanceof ParameterizedTypeReference<?> typeReference) {
			return responseSpec.toEntity(typeReference);
		}
		else if (expectedResponseType instanceof Class<?> clazz) {
			return responseSpec.toEntity(clazz);
		}
		else {
			throw new IllegalArgumentException("Unsupported expectedResponseType: " + expectedResponseType);
		}
	}

}
