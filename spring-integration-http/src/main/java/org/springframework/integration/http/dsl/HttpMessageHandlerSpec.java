/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.dsl;

import java.net.URI;
import java.util.Arrays;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link HttpRequestExecutingMessageHandler}.
 *
 * @author Artem Bilan
 * @author Shiliang Li
 * @author Oleksii Komlyk
 *
 * @since 5.0
 *
 * @see HttpRequestExecutingMessageHandler
 */
public class HttpMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<HttpMessageHandlerSpec, HttpRequestExecutingMessageHandler> {

	@Nullable
	private final RestTemplate restTemplate;

	protected HttpMessageHandlerSpec(URI uri, @Nullable RestTemplate restTemplate) {
		this(new ValueExpression<>(uri), restTemplate);
	}

	protected HttpMessageHandlerSpec(String uri, @Nullable RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
	}

	protected HttpMessageHandlerSpec(Expression uriExpression, @Nullable RestTemplate restTemplate) {
		super(new HttpRequestExecutingMessageHandler(uriExpression, restTemplate));
		this.restTemplate = restTemplate;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @param requestFactory The request factory.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.isTrue(!isClientSet(), "the 'requestFactory' must be specified on the provided 'restTemplate'");
		this.target.setRequestFactory(requestFactory);
		return this;
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 * @param errorHandler The error handler.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec errorHandler(ResponseErrorHandler errorHandler) {
		Assert.isTrue(!isClientSet(), "the 'errorHandler' must be specified on the provided 'restTemplate'");
		this.target.setErrorHandler(errorHandler);
		return _this();
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 * @param messageConverters The message converters.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
		Assert.isTrue(!isClientSet(), "the 'messageConverters' must be specified on the provided 'restTemplate'");
		this.target.setMessageConverters(Arrays.asList(messageConverters));
		return _this();
	}

	@Override
	protected boolean isClientSet() {
		return this.restTemplate != null;
	}

}
