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
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import reactor.core.publisher.Mono;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to a Reactive {@link WebClient} instance.
 *
 * @author Shiliang Li
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see HttpRequestExecutingMessageHandler
 */
public class ReactiveHttpRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {

	private final WebClient webClient;

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public ReactiveHttpRequestExecutingMessageHandler(URI uri) {
		this(new ValueExpression<>(uri));
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public ReactiveHttpRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 * @param uriExpression The URI expression.
	 */
	public ReactiveHttpRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided WebClient.
	 * @param uri The URI.
	 * @param webClient The WebClient to use.
	 */
	public ReactiveHttpRequestExecutingMessageHandler(String uri, WebClient webClient) {
		this(new LiteralExpression(uri), webClient);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided WebClient.
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link BeanFactory}.
	 * @param webClient The WebClient to use.
	 */
	public ReactiveHttpRequestExecutingMessageHandler(Expression uriExpression, WebClient webClient) {
		super(uriExpression);
		this.webClient = (webClient == null ? WebClient.create() : webClient);
		this.setAsync(true);
	}

	@Override
	public String getComponentType() {
		return (isExpectReply() ? "http:outbound-reactive-gateway" : "http:outbound-reactive-channel-adapter");
	}

	@Override
	protected Object exchange(Supplier<URI> uriSupplier, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Message<?> requestMessage) {

		WebClient.RequestBodySpec requestSpec =
				this.webClient.method(httpMethod)
						.uri(b -> uriSupplier.get())
						.headers(httpRequest.getHeaders());

		if (httpRequest.hasBody()) {
			requestSpec.body(BodyInserters.fromObject(httpRequest.getBody()));
		}

		Mono<ClientResponse> responseMono = requestSpec.exchange()
				.doOnNext(response -> {
					HttpStatus httpStatus = response.statusCode();
					if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
						throw new WebClientException(
								"ClientResponse has erroneous status code: " + httpStatus.value() +
										" " + httpStatus.getReasonPhrase());
					}
				});

		if (isExpectReply()) {
			ResolvableType responseType;

			if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
				responseType = ResolvableType.forType(((ParameterizedTypeReference<?>) expectedResponseType).getType());
			}
			else if (expectedResponseType != null) {
				responseType = ResolvableType.forClass((Class<?>) expectedResponseType);
			}
			else {
				responseType = null;
			}

			return responseMono
					.map(response ->
							new ResponseEntity<>(responseType != null
									? response.body(BodyExtractors.toMono(responseType)).block()
									: null,
									response.headers().asHttpHeaders(),
									response.statusCode()))
					.map(this::getReply);
		}
		else {
			responseMono.subscribe(v -> { }, ex -> sendErrorMessage(requestMessage, ex));

			return null;
		}
	}

}
