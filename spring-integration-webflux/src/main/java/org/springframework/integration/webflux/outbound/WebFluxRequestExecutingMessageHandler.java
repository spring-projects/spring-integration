/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.webflux.outbound;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.AbstractHttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
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
 * @see org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler
 */
public class WebFluxRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {

	private final WebClient webClient;

	private boolean replyPayloadToFlux;

	private BodyExtractor<?, ClientHttpResponse> bodyExtractor;

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public WebFluxRequestExecutingMessageHandler(URI uri) {
		this(new ValueExpression<>(uri));
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 * @param uri The URI.
	 */
	public WebFluxRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 * @param uriExpression The URI expression.
	 */
	public WebFluxRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided WebClient.
	 * @param uri The URI.
	 * @param webClient The WebClient to use.
	 */
	public WebFluxRequestExecutingMessageHandler(String uri, WebClient webClient) {
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
	public WebFluxRequestExecutingMessageHandler(Expression uriExpression, WebClient webClient) {
		super(uriExpression);
		this.webClient = (webClient == null ? WebClient.create() : webClient);
		this.setAsync(true);
	}

	/**
	 * The boolean flag to identify if the reply payload should be as a {@link Flux} from the response body
	 * or as resolved value from the {@link Mono} of the response body.
	 * Defaults to {@code false} - simple value is pushed downstream.
	 * Makes sense when {@code expectedResponseType} is configured.
	 * @param replyPayloadToFlux represent reply payload as a {@link Flux} or as a value from the {@link Mono}.
	 * @since 5.0.1
	 * @see #setExpectedResponseType(Class)
	 * @see #setExpectedResponseTypeExpression(Expression)
	 */
	public void setReplyPayloadToFlux(boolean replyPayloadToFlux) {
		this.replyPayloadToFlux = replyPayloadToFlux;
	}

	/**
	 * Specify a {@link BodyExtractor} as an alternative to the {@code expectedResponseType}
	 * to allow to get low-level access to the received {@link ClientHttpResponse}.
	 * @param bodyExtractor the {@link BodyExtractor} to use.
	 * @since 5.0.1
	 * @see #setExpectedResponseType(Class)
	 * @see #setExpectedResponseTypeExpression(Expression)
	 */
	public void setBodyExtractor(BodyExtractor<?, ClientHttpResponse> bodyExtractor) {
		this.bodyExtractor = bodyExtractor;
	}

	@Override
	public String getComponentType() {
		return (isExpectReply() ? "webflux:outbound-gateway" : "webflux:outbound-channel-adapter");
	}

	@Override
	protected Object exchange(Supplier<URI> uriSupplier, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Message<?> requestMessage) {

		WebClient.RequestBodySpec requestSpec =
				this.webClient.method(httpMethod)
						.uri(b -> uriSupplier.get())
						.headers(headers -> headers.putAll(httpRequest.getHeaders()));

		if (httpRequest.hasBody()) {
			requestSpec.body(BodyInserters.fromObject(httpRequest.getBody()));
		}

		Mono<ClientResponse> responseMono =
				requestSpec.exchange()
						.flatMap(response -> {
							HttpStatus httpStatus = response.statusCode();
							if (httpStatus.isError()) {
								return response.body(BodyExtractors.toDataBuffers())
										.reduce(DataBuffer::write)
										.map(dataBuffer -> {
											byte[] bytes = new byte[dataBuffer.readableByteCount()];
											dataBuffer.read(bytes);
											DataBufferUtils.release(dataBuffer);
											return bytes;
										})
										.defaultIfEmpty(new byte[0])
										.map(bodyBytes -> {
													throw new WebClientResponseException(
															"ClientResponse has erroneous status code: "
																	+ httpStatus.value() + " "
																	+ httpStatus.getReasonPhrase(),
															httpStatus.value(),
															httpStatus.getReasonPhrase(),
															response.headers().asHttpHeaders(),
															bodyBytes,
															response.headers().contentType()
																	.map(MimeType::getCharset)
																	.orElse(StandardCharsets.ISO_8859_1));
												}
										);
							}
							else {
								return Mono.just(response);
							}
						});

		if (isExpectReply()) {
			return responseMono
					.flatMap(response -> {
								ResponseEntity.BodyBuilder httpEntityBuilder =
										ResponseEntity.status(response.statusCode())
												.headers(response.headers().asHttpHeaders());

								Mono<?> bodyMono;

								if (expectedResponseType != null) {
									if (this.replyPayloadToFlux) {
										BodyExtractor<? extends Flux<?>, ReactiveHttpInputMessage> extractor;
										if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
											extractor = BodyExtractors.toFlux(
													(ParameterizedTypeReference<?>) expectedResponseType);
										}
										else {
											extractor = BodyExtractors.toFlux((Class<?>) expectedResponseType);
										}
										Flux<?> flux = response.body(extractor);
										bodyMono = Mono.just(flux);
									}
									else {
										BodyExtractor<? extends Mono<?>, ReactiveHttpInputMessage> extractor;
										if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
											extractor = BodyExtractors.toMono(
													(ParameterizedTypeReference<?>) expectedResponseType);
										}
										else {
											extractor = BodyExtractors.toMono((Class<?>) expectedResponseType);
										}
										bodyMono = response.body(extractor);
									}
								}
								else if (this.bodyExtractor != null) {
									Object body = response.body(this.bodyExtractor);
									if (body instanceof Mono) {
										bodyMono = (Mono<?>) body;
									}
									else {
										bodyMono = Mono.just(body);
									}
								}
								else {
									bodyMono = Mono.empty();
								}

								return bodyMono
										.map(httpEntityBuilder::body)
										.defaultIfEmpty(httpEntityBuilder.build());
							}
					)
					.map(this::getReply);
		}
		else {
			responseMono.subscribe(v -> { }, ex -> sendErrorMessage(requestMessage, ex));

			return null;
		}
	}

}
