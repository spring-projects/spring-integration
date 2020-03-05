/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.integration.webflux.outbound;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.AbstractHttpRequestExecutingMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that executes
 * HTTP requests by delegating to a Reactive {@link WebClient} instance.
 *
 * @author Shiliang Li
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @see org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler
 * @see WebClient
 */
public class WebFluxRequestExecutingMessageHandler extends AbstractHttpRequestExecutingMessageHandler {

	private final WebClient webClient;

	private final boolean webClientExplicitlySet;

	private boolean replyPayloadToFlux;

	private BodyExtractor<?, ClientHttpResponse> bodyExtractor;

	private Expression publisherElementTypeExpression;

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
	public WebFluxRequestExecutingMessageHandler(String uri, @Nullable WebClient webClient) {
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
	 * {@link org.springframework.beans.factory.BeanFactory}.
	 * @param webClient The WebClient to use.
	 */
	public WebFluxRequestExecutingMessageHandler(Expression uriExpression, @Nullable WebClient webClient) {
		super(uriExpression);
		this.webClientExplicitlySet = webClient != null;
		this.webClient =
				!this.webClientExplicitlySet
						? WebClient.builder().uriBuilderFactory(this.uriFactory).build()
						: webClient;
		this.setAsync(true);
	}

	private void assertLocalWebClient(String option) {
		Assert.isTrue(!this.webClientExplicitlySet,
				() -> "The option '" + option + "' must be provided on the externally configured WebClient: "
						+ this.webClient);
	}

	@Override
	public void setEncodingMode(DefaultUriBuilderFactory.EncodingMode encodingMode) {
		assertLocalWebClient("encodingMode on UriBuilderFactory");
		super.setEncodingMode(encodingMode);
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

	/**
	 * Configure a type for a request {@link Publisher} elements.
	 * @param publisherElementType the type of the request {@link Publisher} elements.
	 * @since 5.2
	 * @see BodyInserters#fromPublisher(Publisher, Class)
	 */
	public void setPublisherElementType(Class<?> publisherElementType) {
		Assert.notNull(publisherElementType, "'publisherElementType' must not be null");
		setPublisherElementTypeExpression(new ValueExpression<>(publisherElementType));

	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link Publisher} elements type at runtime against
	 * a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link Publisher} elements.
	 * @since 5.2
	 * @see BodyInserters#fromPublisher(Publisher, Class)
	 * @see BodyInserters#fromPublisher(Publisher, ParameterizedTypeReference)
	 */
	public void setPublisherElementTypeExpression(Expression publisherElementTypeExpression) {
		this.publisherElementTypeExpression = publisherElementTypeExpression;
	}

	@Override
	public String getComponentType() {
		return (isExpectReply() ? "webflux:outbound-gateway" : "webflux:outbound-channel-adapter");
	}

	@Override
	@Nullable
	protected Object exchange(Object uri, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Message<?> requestMessage, Map<String, ?> uriVariables) {

		WebClient.RequestBodySpec requestSpec =
				createRequestBodySpec(uri, httpMethod, httpRequest, requestMessage, uriVariables);

		Mono<ClientResponse> responseMono = exchangeForResponseMono(requestSpec);

		if (isExpectReply()) {
			return createReplyFromResponse(expectedResponseType, responseMono);
		}
		else {
			return responseMono.then();
		}
	}

	private Object createReplyFromResponse(Object expectedResponseType, Mono<ClientResponse> responseMono) {
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

	private WebClient.RequestBodySpec createRequestBodySpec(Object uri, HttpMethod httpMethod,
			HttpEntity<?> httpRequest, Message<?> requestMessage, Map<String, ?> uriVariables) {

		WebClient.RequestBodyUriSpec requestBodyUriSpec = this.webClient.method(httpMethod);
		WebClient.RequestBodySpec requestSpec;

		if (uri instanceof URI) {
			requestSpec = requestBodyUriSpec.uri((URI) uri);
		}
		else {
			requestSpec = requestBodyUriSpec.uri((String) uri, uriVariables);
		}

		requestSpec = requestSpec.headers(headers -> headers.putAll(httpRequest.getHeaders()));
		BodyInserter<?, ? super ClientHttpRequest> inserter = buildBodyInserterForRequest(requestMessage, httpRequest);
		if (inserter != null) {
			requestSpec.body(inserter);
		}
		return requestSpec;
	}

	private Mono<ClientResponse> exchangeForResponseMono(WebClient.RequestBodySpec requestSpec) {
		return requestSpec.exchange()
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
	}

	@Nullable
	private BodyInserter<?, ? super ClientHttpRequest> buildBodyInserterForRequest(Message<?> requestMessage,
			HttpEntity<?> httpRequest) {

		Object requestBody = httpRequest.getBody();
		if (requestBody == null) {
			return null;
		}

		BodyInserter<?, ? super ClientHttpRequest> inserter = null;
		if (requestBody instanceof Resource) {
			inserter = BodyInserters.fromResource((Resource) requestBody);
		}
		else if (requestBody instanceof Publisher<?>) {
			inserter = buildBodyInserterForPublisher(requestMessage, (Publisher<?>) requestBody);
		}
		else if (requestBody instanceof MultiValueMap<?, ?>) {
			inserter = buildBodyInserterForMultiValueMap((MultiValueMap<?, ?>) requestBody,
					httpRequest.getHeaders().getContentType());
		}
		else {
			inserter = BodyInserters.fromValue(requestBody);
		}
		return inserter;
	}

	@SuppressWarnings("unchecked")
	private <T, P extends Publisher<T>> BodyInserter<P, ? super ClientHttpRequest> buildBodyInserterForPublisher(
			Message<?> requestMessage, P publisher) {

		BodyInserter<P, ? super ClientHttpRequest> inserter;
		Object publisherElementType = evaluateTypeFromExpression(requestMessage,
				this.publisherElementTypeExpression, "publisherElementType");
		if (publisherElementType instanceof Class<?>) {
			inserter = BodyInserters.fromPublisher(publisher, (Class<T>) publisherElementType);
		}
		else if (publisherElementType instanceof ParameterizedTypeReference<?>) {
			inserter = BodyInserters.fromPublisher(publisher, (ParameterizedTypeReference<T>) publisherElementType);
		}
		else {
			inserter = BodyInserters.fromPublisher(publisher, (Class<T>) Object.class);
		}
		return inserter;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static BodyInserters.FormInserter<?> buildBodyInserterForMultiValueMap(
			MultiValueMap<?, ?> requestBody, MediaType contentType) {

		if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType)) {
			return BodyInserters.fromFormData((MultiValueMap<String, String>) requestBody);
		}
		else if (MediaType.MULTIPART_FORM_DATA.equals(contentType)) {
			return BodyInserters.fromMultipartData((MultiValueMap<String, ?>) requestBody);
		}
		else {
			return null;
		}
	}

}
