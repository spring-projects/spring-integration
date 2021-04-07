/*
 * Copyright 2017-2021 the original author or authors.
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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.http.HttpHeaders;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.WebHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * A {@link org.springframework.integration.gateway.MessagingGatewaySupport}
 * implementation for Spring WebFlux HTTP requests execution.
 *
 * @author Artem Bilan
 * @author Gary Russell
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
	public Mono<Void> handle(ServerWebExchange exchange) {
		return Mono.defer(() -> {
			if (isRunning()) {
				return doHandle(exchange);
			}
			else {
				return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Endpoint is stopped"))
						.then();
			}
		});
	}

	private Mono<Void> doHandle(ServerWebExchange exchange) {
		return extractRequestBody(exchange)
				.doOnSubscribe(s -> this.activeCount.incrementAndGet())
				.map(body ->
						new RequestEntity<>(body, exchange.getRequest().getHeaders(),
								exchange.getRequest().getMethod(), exchange.getRequest().getURI()))
				.flatMap(entity -> buildMessage(entity, exchange))
				.flatMap(requestTuple -> {
					if (isExpectReply()) {
						return sendAndReceiveMessageReactive(requestTuple.getT1())
								.flatMap(replyMessage -> populateResponse(exchange, replyMessage));
					}
					else {
						send(requestTuple.getT1());
						return setStatusCode(exchange, requestTuple.getT2());
					}
				})
				.doOnTerminate(this.activeCount::decrementAndGet);

	}

	private Mono<?> extractRequestBody(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		if (isReadable(request.getMethod())) {
			return extractReadableRequestBody(exchange)
					.cast(Object.class)
					.switchIfEmpty(queryParams(request));
		}
		else {
			return queryParams(request);
		}
	}

	private Mono<?> extractReadableRequestBody(ServerWebExchange exchange) {
		MediaType contentType =
				exchange.getRequest()
						.getHeaders()
						.getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
			return exchange.getFormData();
		}
		else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
			return exchange.getMultipartData();
		}
		else {
			return readRequestBody(exchange, contentType);
		}
	}

	private Mono<?> readRequestBody(ServerWebExchange exchange, MediaType contentType) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		ResolvableType bodyType = getRequestPayloadType();
		if (bodyType == null) {
			bodyType =
					"text".equals(contentType.getType())
							? ResolvableType.forClass(String.class)
							: ResolvableType.forClass(byte[].class);
		}

		Class<?> resolvedType = bodyType.resolve();

		ReactiveAdapter adapter =
				resolvedType != null
						? this.adapterRegistry.getAdapter(resolvedType)
						: null;
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
			if (getValidator() != null) {
				flux = flux.doOnNext(this::validate);
			}
			return Mono.just(adapter.fromPublisher(flux));
		}
		else {
			Mono<?> mono = httpMessageReader.readMono(bodyType, elementType, request, response, readHints);
			if (getValidator() != null) {
				mono = mono.doOnNext(this::validate);
			}
			if (adapter != null) {
				return Mono.just(adapter.fromPublisher(mono));
			}
			else {
				return mono;
			}
		}
	}

	private Mono<Tuple2<Message<Object>, RequestEntity<?>>> buildMessage(RequestEntity<?> httpEntity,
			ServerWebExchange exchange) {

		ServerHttpRequest request = exchange.getRequest();
		MultiValueMap<String, String> requestParams = request.getQueryParams();

		EvaluationContext evaluationContext = buildEvaluationContext(httpEntity, exchange);
		Object payload;
		if (getPayloadExpression() != null) {
			payload = getPayloadExpression().getValue(evaluationContext);
		}
		else {
			payload = httpEntity.getBody();
		}

		Map<String, Object> headers = getHeaderMapper().toHeaders(request.getHeaders());
		if (!CollectionUtils.isEmpty(getHeaderExpressions())) {
			headers.putAll(
					ExpressionEvalMap.from(getHeaderExpressions())
							.usingEvaluationContext(evaluationContext)
							.withRoot(httpEntity)
							.build());
		}

		if (payload == null) {
			payload = requestParams;
		}

		AbstractIntegrationMessageBuilder<Object> messageBuilder =
				prepareRequestMessageBuilder(request, payload, headers);

		return exchange.getPrincipal()
				.map(principal -> messageBuilder.setHeader(HttpHeaders.USER_PRINCIPAL, principal))
				.defaultIfEmpty(messageBuilder)
				.map(AbstractIntegrationMessageBuilder::build)
				.zipWith(Mono.just(httpEntity));
	}

	@SuppressWarnings("unchecked")
	private AbstractIntegrationMessageBuilder<Object> prepareRequestMessageBuilder(ServerHttpRequest request,
			Object payload, Map<String, Object> headers) {

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

		messageBuilder.setHeader(HttpHeaders.REQUEST_URL, request.getURI().toString());
		HttpMethod httpMethod = request.getMethod();
		if (httpMethod != null) {
			messageBuilder.setHeader(HttpHeaders.REQUEST_METHOD, httpMethod.toString());
		}
		return messageBuilder;
	}

	private EvaluationContext buildEvaluationContext(RequestEntity<?> httpEntity, ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		org.springframework.http.HttpHeaders requestHeaders = request.getHeaders();
		MultiValueMap<String, String> requestParams = request.getQueryParams();
		Map<String, Object> exchangeAttributes = exchange.getAttributes();

		StandardEvaluationContext evaluationContext = createEvaluationContext();

		evaluationContext.setVariable("requestAttributes", exchangeAttributes);
		evaluationContext.setVariable("requestParams", requestParams);
		evaluationContext.setVariable("requestHeaders", requestHeaders);
		if (!CollectionUtils.isEmpty(request.getCookies())) {
			evaluationContext.setVariable("cookies", request.getCookies());
		}

		Map<?, ?> pathVariables = (Map<?, ?>) exchangeAttributes.get(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(pathVariables)) {
			evaluationContext.setVariable("pathVariables", pathVariables);
		}

		Map<?, ?> matrixVariables = (Map<?, ?>) exchangeAttributes.get(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(matrixVariables)) {
			evaluationContext.setVariable("matrixVariables", matrixVariables);
		}

		evaluationContext.setRootObject(httpEntity);
		return evaluationContext;
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

							org.springframework.http.HttpHeaders entityHeaders = e.getHeaders();
							org.springframework.http.HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

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

	private Mono<Void> setStatusCode(ServerWebExchange exchange, RequestEntity<?> requestEntity) {
		ServerHttpResponse response = exchange.getResponse();
		if (getStatusCodeExpression() != null) {
			HttpStatus httpStatus = evaluateHttpStatus(requestEntity);
			if (httpStatus != null) {
				response.setStatusCode(httpStatus);
			}
		}
		return response.setComplete();
	}

	private static ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType genericType) {
		if (adapter.isNoValue()) {
			return ResolvableType.forClass(Void.class);
		}
		else if (!ResolvableType.NONE.equals(genericType)) {
			return genericType;
		}
		else {
			return ResolvableType.forClass(Object.class);
		}
	}

	private static List<MediaType> getProducibleTypes(ServerWebExchange exchange,
			Supplier<List<MediaType>> producibleTypesSupplier) {

		Set<MediaType> mediaTypes = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return (mediaTypes != null ? new ArrayList<>(mediaTypes) : producibleTypesSupplier.get());
	}

	private static Mono<?> queryParams(ServerHttpRequest request) {
		return Mono.just(request.getQueryParams());
	}

	private static MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		MediaType producibleToUse = producible.copyQualityValue(acceptable);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptable, producibleToUse) <= 0 ? acceptable : producibleToUse);
	}

}
