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

package org.springframework.integration.webflux.inbound;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.inbound.BaseHttpInboundEndpoint;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.WebHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@link MessagingGatewaySupport} implementation for Spring WebFlux
 * HTTP requests execution.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see org.springframework.web.reactive.result.HandlerResultHandlerSupport
 * @see org.springframework.web.reactive.config.EnableWebFlux
 */
public class WebFluxInboundEndpoint extends BaseHttpInboundEndpoint implements WebHandler {

	private static final MediaType MEDIA_TYPE_APPLICATION_ALL = new MediaType("application");

	private static final List<HttpMethod> SAFE_METHODS = Arrays.asList(HttpMethod.GET, HttpMethod.HEAD);

	private ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

	private RequestedContentTypeResolver requestedContentTypeResolver = new HeaderContentTypeResolver();

	private ReactiveAdapterRegistry adapterRegistry = new ReactiveAdapterRegistry();

	public WebFluxInboundEndpoint() {
		this(true);
	}

	public WebFluxInboundEndpoint(boolean expectReply) {
		super(expectReply);
	}

	/**
	 * A {@link ServerCodecConfigurer} for the request readers and response writers.
	 * By default the {@link ServerCodecConfigurer#create()} factory is used.
	 * @param codecConfigurer the {@link ServerCodecConfigurer} to use.
	 */
	public void setCodecConfigurer(ServerCodecConfigurer codecConfigurer) {
		Assert.notNull(codecConfigurer, "'codecConfigurer' must not be null");
		this.codecConfigurer = codecConfigurer;
	}

	/**
	 * A strategy to resolve the requested media types for a {@code ServerWebExchange}.
	 * A {@link HeaderContentTypeResolver} is used by default.
	 * @param requestedContentTypeResolver the {@link RequestedContentTypeResolver} to use.
	 */
	public void setRequestedContentTypeResolver(RequestedContentTypeResolver requestedContentTypeResolver) {
		Assert.notNull(requestedContentTypeResolver, "'requestedContentTypeResolver' must not be null");
		this.requestedContentTypeResolver = requestedContentTypeResolver;
	}

	/**
	 * A registry of adapters to adapt a Reactive Streams {@link Publisher} to/from.
	 * @param adapterRegistry the {@link ReactiveAdapterRegistry} to use.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(adapterRegistry, "'adapterRegistry' must not be null");
		this.adapterRegistry = adapterRegistry;
	}

	@Override
	public String getComponentType() {
		return super.getComponentType().replaceFirst("http", "webflux");
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return Mono.defer(() -> {
			if (isRunning()) {
				return doHandle(exchange);
			}
			else {
				return serviceUnavailableResponse(exchange);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> doHandle(ServerWebExchange exchange) {
		return extractRequestBody(exchange)
				.doOnSubscribe(s -> this.activeCount.incrementAndGet())
				.switchIfEmpty(Mono.just(exchange.getRequest().getQueryParams()))
				.map(body -> new HttpEntity<>(body, exchange.getRequest().getHeaders()))
				.flatMap(entity -> buildMessage(entity, exchange))
				.flatMap(requestMessage -> {
					if (this.expectReply) {
						return sendAndReceiveMessageReactive(requestMessage)
								.flatMap(replyMessage -> populateResponse(exchange, replyMessage));
					}
					else {
						send(requestMessage);
						return setStatusCode(exchange);
					}
				})
				.doOnTerminate(this.activeCount::decrementAndGet);

	}

	@SuppressWarnings("unchecked")
	private <T> Mono<T> extractRequestBody(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		if (isReadable(request)) {
			MediaType contentType;
			if (request.getHeaders().getContentType() == null) {
				contentType = MediaType.APPLICATION_OCTET_STREAM;
			}
			else {
				contentType = request.getHeaders().getContentType();
			}

			if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
				return (Mono<T>) exchange.getFormData();
			}
			else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
				return (Mono<T>) exchange.getMultipartData();
			}
			else {
				ResolvableType bodyType = getRequestPayloadType();
				if (bodyType == null) {
					bodyType =
							"text".equals(contentType.getType())
									? ResolvableType.forClass(String.class)
									: ResolvableType.forClass(byte[].class);
				}

				Class<?> resolvedType = bodyType.resolve();

				ReactiveAdapter adapter = (resolvedType != null ? this.adapterRegistry.getAdapter(resolvedType) : null);
				ResolvableType elementType = (adapter != null ? bodyType.getGeneric() : bodyType);

				HttpMessageReader<?> httpMessageReader = this.codecConfigurer
						.getReaders()
						.stream()
						.filter(reader -> reader.canRead(elementType, contentType))
						.findFirst()
						.orElseThrow(() -> new UnsupportedMediaTypeStatusException(
								"Could not convert request: no suitable HttpMessageReader found for expected type ["
										+ elementType + "] and content type [" + contentType + "]"));


				Map<String, Object> readHints = Collections.emptyMap();
				if (adapter != null && adapter.isMultiValue()) {
					Flux<?> flux = httpMessageReader.read(bodyType, elementType, request, response, readHints);

					return (Mono<T>) Mono.just(adapter.fromPublisher(flux));
				}
				else {
					Mono<?> mono = httpMessageReader.readMono(bodyType, elementType, request, response, readHints);

					if (adapter != null) {
						return (Mono<T>) Mono.just(adapter.fromPublisher(mono));
					}
					else {
						return (Mono<T>) mono;
					}
				}
			}
		}
		else {
			return (Mono<T>) Mono.just(exchange.getRequest().getQueryParams());
		}
	}

	@SuppressWarnings("unchecked")
	private Mono<Message<?>> buildMessage(HttpEntity<?> httpEntity, ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders requestHeaders = request.getHeaders();
		Map<String, Object> exchangeAttributes = exchange.getAttributes();

		StandardEvaluationContext evaluationContext = createEvaluationContext();

		evaluationContext.setVariable("requestAttributes", exchangeAttributes);
		MultiValueMap<String, String> requestParams = request.getQueryParams();
		evaluationContext.setVariable("requestParams", requestParams);
		evaluationContext.setVariable("requestHeaders", requestHeaders);
		if (!CollectionUtils.isEmpty(request.getCookies())) {
			evaluationContext.setVariable("cookies", request.getCookies());
		}

		Map<String, String> pathVariables =
				(Map<String, String>) exchangeAttributes.get(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(pathVariables)) {
			evaluationContext.setVariable("pathVariables", pathVariables);
		}

		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) exchangeAttributes.get(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(matrixVariables)) {
			evaluationContext.setVariable("matrixVariables", matrixVariables);
		}

		evaluationContext.setRootObject(httpEntity);
		Object payload;
		if (getPayloadExpression() != null) {
			payload = getPayloadExpression().getValue(evaluationContext);
		}
		else {
			payload = httpEntity.getBody();
		}

		Map<String, Object> headers = getHeaderMapper().toHeaders(request.getHeaders());
		if (!CollectionUtils.isEmpty(getHeaderExpressions())) {
			for (Map.Entry<String, Expression> entry : getHeaderExpressions().entrySet()) {
				String headerName = entry.getKey();
				Expression headerExpression = entry.getValue();
				Object headerValue = headerExpression.getValue(evaluationContext);
				if (headerValue != null) {
					headers.put(headerName, headerValue);
				}
			}
		}

		if (payload == null) {
			payload = requestParams;
		}

		AbstractIntegrationMessageBuilder<Object> messageBuilder;

		if (payload instanceof Message<?>) {
			messageBuilder =
					getMessageBuilderFactory()
							.fromMessage((Message<Object>) payload)
							.copyHeadersIfAbsent(headers);
		}
		else {
			messageBuilder =
					getMessageBuilderFactory()
							.withPayload(payload)
							.copyHeaders(headers);
		}

		messageBuilder
				.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_URL, request.getURI().toString())
				.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_METHOD,
						request.getMethod().toString());

		return exchange.getPrincipal()
				.map(principal ->
						messageBuilder
								.setHeader(org.springframework.integration.http.HttpHeaders.USER_PRINCIPAL, principal))
				.defaultIfEmpty(messageBuilder)
				.map(AbstractIntegrationMessageBuilder::build);
	}

	private Mono<Void> populateResponse(ServerWebExchange exchange, Message<?> replyMessage) {
		ServerHttpResponse response = exchange.getResponse();
		getHeaderMapper().fromHeaders(replyMessage.getHeaders(), response.getHeaders());

		Object responseContent = replyMessage;
		if (getExtractReplyPayload()) {
			responseContent = replyMessage.getPayload();
		}

		if (responseContent instanceof HttpStatus) {
			response.setStatusCode((HttpStatus) responseContent);
			return response.setComplete();
		}
		else {
			final HttpStatus httpStatus = resolveHttpStatusFromHeaders(replyMessage.getHeaders());
			if (httpStatus != null) {
				response.setStatusCode(httpStatus);
			}

			if (responseContent instanceof ResponseEntity) {
				return Mono.just((ResponseEntity<?>) responseContent)
						.flatMap(e -> {
							if (httpStatus == null) {
								exchange.getResponse().setStatusCode(e.getStatusCode());
							}

							HttpHeaders entityHeaders = e.getHeaders();
							HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

							if (!entityHeaders.isEmpty()) {
								entityHeaders.entrySet().stream()
										.filter(entry -> !responseHeaders.containsKey(entry.getKey()))
										.forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
							}

							if (e.getBody() == null) {
								return exchange.getResponse().setComplete();
							}

							String etag = entityHeaders.getETag();
							Instant lastModified = Instant.ofEpochMilli(entityHeaders.getLastModified());
							HttpMethod httpMethod = exchange.getRequest().getMethod();
							if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(etag, lastModified)) {
								return exchange.getResponse().setComplete();
							}

							return writeResponseBody(exchange, e.getBody());
						});
			}
			else {
				return writeResponseBody(exchange, responseContent);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> writeResponseBody(ServerWebExchange exchange, Object body) {
		ResolvableType bodyType = ResolvableType.forInstance(body);
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(bodyType.resolve(), body);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(body);
			ResolvableType genericType = bodyType.getGeneric(0);
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(body);
			elementType = bodyType;
		}

		if (void.class == elementType.getRawClass() || Void.class == elementType.getRawClass()) {
			return Mono.from((Publisher<Void>) publisher);
		}

		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(bodyType);
		MediaType bestMediaType = selectMediaType(exchange, () -> producibleMediaTypes);

		if (bestMediaType != null) {
			for (HttpMessageWriter<?> writer : this.codecConfigurer.getWriters()) {
				if (writer.canWrite(bodyType, bestMediaType)) {
					return ((HttpMessageWriter<Object>) writer).write(publisher, elementType,
							bestMediaType, exchange.getResponse(), Collections.emptyMap());
				}
			}
		}
		else {
			if (producibleMediaTypes.isEmpty()) {
				return Mono.error(new IllegalStateException("No HttpMessageWriters for response type: " + bodyType));
			}
		}

		return Mono.error(new NotAcceptableStatusException(producibleMediaTypes));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType genericType) {
		if (adapter.isNoValue()) {
			return ResolvableType.forClass(Void.class);
		}
		else if (genericType != ResolvableType.NONE) {
			return genericType;
		}
		else {
			return ResolvableType.forClass(Object.class);
		}
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType elementType) {
		return this.codecConfigurer.getWriters()
				.stream()
				.filter(converter -> converter.canWrite(elementType, null))
				.flatMap(converter -> converter.getWritableMediaTypes().stream())
				.collect(Collectors.toList());
	}

	private MediaType selectMediaType(ServerWebExchange exchange, Supplier<List<MediaType>> producibleTypesSupplier) {
		List<MediaType> acceptableTypes = getAcceptableTypes(exchange);
		List<MediaType> producibleTypes = getProducibleTypes(exchange, producibleTypesSupplier);

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptable : acceptableTypes) {
			for (MediaType producible : producibleTypes) {
				if (acceptable.isCompatibleWith(producible)) {
					compatibleMediaTypes.add(selectMoreSpecificMediaType(acceptable, producible));
				}
			}
		}

		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(result);

		for (MediaType mediaType : result) {
			if (mediaType.isConcrete()) {
				return mediaType;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION_ALL)) {
				return MediaType.APPLICATION_OCTET_STREAM;
			}
		}

		return null;
	}

	private List<MediaType> getAcceptableTypes(ServerWebExchange exchange) {
		List<MediaType> mediaTypes = this.requestedContentTypeResolver.resolveMediaTypes(exchange);
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleTypes(ServerWebExchange exchange,
			Supplier<List<MediaType>> producibleTypesSupplier) {

		Set<MediaType> mediaTypes = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return (mediaTypes != null ? new ArrayList<>(mediaTypes) : producibleTypesSupplier.get());
	}

	private MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		producible = producible.copyQualityValue(acceptable);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptable, producible) <= 0 ? acceptable : producible);
	}


	private Mono<Void> setStatusCode(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		if (getStatusCodeExpression() != null) {
			HttpStatus httpStatus = evaluateHttpStatus();
			if (httpStatus != null) {
				response.setStatusCode(httpStatus);
			}
		}

		return response.setComplete();
	}

	private Mono<Void> serviceUnavailableResponse(ServerWebExchange exchange) {
		if (logger.isDebugEnabled()) {
			logger.debug("Endpoint is stopped; returning status " + HttpStatus.SERVICE_UNAVAILABLE);
		}
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
		return response.writeWith(
				Mono.just(response.bufferFactory()
						.wrap("Endpoint is stopped".getBytes())));
	}

}
