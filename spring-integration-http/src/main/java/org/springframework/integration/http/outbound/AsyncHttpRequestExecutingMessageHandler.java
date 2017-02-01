/*
 * Copyright 2017 the original author or authors.
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

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to an {@link AsyncRestTemplate} instance.
 * @see HttpRequestExecutingMessageHandler
 * @author Shiliang Li
 * @since 5.0
 */
public class AsyncHttpRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {

	private final AsyncRestTemplate asyncRestTemplate;

	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public AsyncHttpRequestExecutingMessageHandler(URI uri) {
		this(new ValueExpression<>(uri));
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public AsyncHttpRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 *
	 * @param uriExpression The URI expression.
	 */
	public AsyncHttpRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided AsyncRestTemplate
	 * @param uri The URI.
	 * @param asyncRestTemplate The rest template.
	 */
	public AsyncHttpRequestExecutingMessageHandler(String uri, AsyncRestTemplate asyncRestTemplate) {
		this(new LiteralExpression(uri), asyncRestTemplate);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided AsyncRestTemplate
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link BeanFactory}.
	 * @param asyncRestTemplate The rest template.
	 */
	public AsyncHttpRequestExecutingMessageHandler(Expression uriExpression, AsyncRestTemplate asyncRestTemplate) {
		super(uriExpression);
		this.asyncRestTemplate = (asyncRestTemplate == null ? new AsyncRestTemplate() : asyncRestTemplate);
		this.setAsync(true);
	}

	@Override
	public String getComponentType() {
		return (this.getExpectReply() ? "http:outbound-async-gateway" : "http:outbound-async-channel-adapter");
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link AsyncRestTemplate}.
	 *
	 * @param errorHandler The error handler.
	 *
	 * @see AsyncRestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	@Override
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.asyncRestTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link AsyncRestTemplate}.
	 * Converters configured via this method will override the default converters.
	 *
	 * @param messageConverters The message converters.
	 *
	 * @see AsyncRestTemplate#setMessageConverters(java.util.List)
	 */
	@Override
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.asyncRestTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * Set the {@link AsyncClientHttpRequestFactory} for the underlying {@link AsyncRestTemplate}.
	 *
	 * @param asyncRequestFactory The request factory.
	 *
	 * @see AsyncRestTemplate#setAsyncRequestFactory(AsyncClientHttpRequestFactory)
	 */
	public void setAsyncRequestFactory(AsyncClientHttpRequestFactory asyncRequestFactory) {
		this.asyncRestTemplate.setAsyncRequestFactory(asyncRequestFactory);
	}

	@Override
	protected Object exchange(URI uri, HttpMethod httpMethod, HttpEntity<?> httpRequest, Object expectedResponseType) {
		SettableListenableFuture<Object> replyMessageFuture = new SettableListenableFuture<>();
		ListenableFuture<? extends ResponseEntity<?>> responseFuture;
		if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
			responseFuture = this.asyncRestTemplate.exchange(uri, httpMethod, httpRequest, (ParameterizedTypeReference<?>) expectedResponseType);
		}
		else {
			responseFuture = this.asyncRestTemplate.exchange(uri, httpMethod, httpRequest, (Class<?>) expectedResponseType);
		}

		responseFuture.addCallback(
				result -> replyMessageFuture.set(getReply(result)),
				replyMessageFuture::setException);

		return replyMessageFuture;
	}
}
