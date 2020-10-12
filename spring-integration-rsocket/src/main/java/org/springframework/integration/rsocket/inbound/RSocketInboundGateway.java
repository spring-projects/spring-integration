/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.rsocket.inbound;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.rsocket.AbstractRSocketConnector;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.IntegrationRSocketEndpoint;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketPayloadReturnValueHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The {@link MessagingGatewaySupport} implementation for the {@link IntegrationRSocketEndpoint}.
 * Represents an inbound endpoint for RSocket requests.
 * <p>
 * May be configured with the {@link AbstractRSocketConnector} for mapping registration.
 * Or existing {@link AbstractRSocketConnector} bean(s) will perform detection automatically.
 * <p>
 * An inbound {@link DataBuffer} (either single or as a {@link Publisher} element) is
 * converted to the target expected type which can be configured by the
 * {@link #setRequestElementClass} or {@link #setRequestElementType(ResolvableType)}.
 * If it is not configured, then target type is determined by the {@code contentType} header:
 * If it is a {@code text}, then target type is {@link String}, otherwise - {@code byte[]}.
 * <p>
 * An inbound {@link Publisher} is used as is in the message to send payload.
 * It is a target application responsibility to process that payload any possible way.
 * <p>
 * A reply payload is encoded to the {@link Flux} according a type of the payload or a
 * {@link Publisher} element type.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketInboundGateway extends MessagingGatewaySupport implements IntegrationRSocketEndpoint {

	private final String[] path;

	private RSocketInteractionModel[] interactionModels = RSocketInteractionModel.values();

	private RSocketStrategies rsocketStrategies = RSocketStrategies.create();

	@Nullable
	private AbstractRSocketConnector rsocketConnector;

	@Nullable
	private ResolvableType requestElementType;

	private boolean decodeFluxAsUnit;

	/**
	 * Instantiate based on the provided path patterns to map this endpoint for incoming RSocket requests.
	 * @param pathArg the mapping patterns to use.
	 */
	public RSocketInboundGateway(String... pathArg) {
		Assert.notNull(pathArg, "'pathArg' must not be null");
		this.path = Arrays.copyOf(pathArg, pathArg.length);
	}

	/**
	 * Configure an {@link RSocketStrategies} instead of a default one.
	 * Note: if {@link AbstractRSocketConnector} is provided, then its
	 * {@link RSocketStrategies} have a precedence.
	 * @param rsocketStrategies the {@link RSocketStrategies} to use.
	 * @see RSocketStrategies#builder
	 */
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		Assert.notNull(rsocketStrategies, "'rsocketStrategies' must not be null");
		this.rsocketStrategies = rsocketStrategies;
	}

	/**
	 * Provide an {@link AbstractRSocketConnector} reference for an explicit endpoint mapping.
	 * @param rsocketConnector the {@link AbstractRSocketConnector} to use.
	 */
	public void setRSocketConnector(AbstractRSocketConnector rsocketConnector) {
		Assert.notNull(rsocketConnector, "'rsocketConnector' must not be null");
		this.rsocketConnector = rsocketConnector;
	}

	/**
	 * Configure a set of {@link RSocketInteractionModel} this endpoint is mapped onto.
	 * @param interactionModelsArg the {@link RSocketInteractionModel}s for mapping.
	 * @since 5.2.2
	 */
	public void setInteractionModels(RSocketInteractionModel... interactionModelsArg) {
		Assert.notNull(interactionModelsArg, "'interactionModelsArg' must not be null");
		this.interactionModels = Arrays.copyOf(interactionModelsArg, interactionModelsArg.length);
	}

	@Override
	public RSocketInteractionModel[] getInteractionModels() {
		return Arrays.copyOf(this.interactionModels, this.interactionModels.length);
	}

	/**
	 * Get an array of the path patterns this endpoint is mapped onto.
	 * @return the mapping path
	 */
	public String[] getPath() {
		return Arrays.copyOf(this.path, this.path.length);
	}

	/**
	 * Specify a type of payload to be generated when the inbound RSocket request
	 * content is read by the encoders.
	 * By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to {@code byte[].class}.
	 * @param requestElementClass The payload type.
	 */
	public void setRequestElementClass(Class<?> requestElementClass) {
		setRequestElementType(ResolvableType.forClass(requestElementClass));
	}

	/**
	 * Specify the type of payload to be generated when the inbound RSocket request
	 * content is read by the converters/encoders.
	 * By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to {@code byte[].class}.
	 * @param requestElementType The payload type.
	 */
	public void setRequestElementType(ResolvableType requestElementType) {
		this.requestElementType = requestElementType;
	}

	/**
	 * Configure an option to decode an incoming {@link Flux} as a single unit or each its event separately.
	 * Defaults to {@code false} for consistency with Spring Messaging {@code @MessageMapping}.
	 * The target {@link Flux} decoding logic depends on the {@link Decoder} selected.
	 * For example a {@link org.springframework.core.codec.StringDecoder} requires a new line separator to
	 * be present in the stream to indicate a byte buffer end.
	 * @param decodeFluxAsUnit decode incoming {@link Flux} as a single unit or each event separately.
	 * @since 5.3
	 * @see Decoder#decode(Publisher, ResolvableType, MimeType, java.util.Map)
	 */
	public void setDecodeFluxAsUnit(boolean decodeFluxAsUnit) {
		this.decodeFluxAsUnit = decodeFluxAsUnit;
	}

	@Override
	protected void onInit() {
		super.onInit();
		AbstractRSocketConnector rsocketConnectorToUse = this.rsocketConnector;
		if (rsocketConnectorToUse != null) {
			rsocketConnectorToUse.addEndpoint(this);
			this.rsocketStrategies = rsocketConnectorToUse.getRSocketStrategies();
		}
	}

	@Override
	protected void doStart() {
		super.doStart();
		if (this.rsocketConnector instanceof ClientRSocketConnector) {
			((ClientRSocketConnector) this.rsocketConnector).connect();
		}
	}

	@Override
	public Mono<Void> handleMessage(Message<?> requestMessage) {
		if (!isRunning()) {
			return Mono.error(new MessageDeliveryException(requestMessage,
					"The RSocket Inbound Gateway '" + getComponentName() + "' is stopped; " +
							"service for path(s) " + Arrays.toString(this.path) + " is not available at the moment."));
		}

		Mono<Message<?>> requestMono = decodeRequestMessage(requestMessage);
		AtomicReference<Object> replyTo = getReplyToHeader(requestMessage);
		if (replyTo != null) {
			return requestMono
					.flatMap(this::sendAndReceiveMessageReactive)
					.flatMap((replyMessage) -> {
						Flux<DataBuffer> reply = createReply(replyMessage.getPayload(), requestMessage);
						replyTo.set(reply);
						return Mono.empty();
					});
		}
		else {
			return requestMono
					.doOnNext(this::send)
					.then();
		}
	}

	private Mono<Message<?>> decodeRequestMessage(Message<?> requestMessage) {
		Object data = decodePayload(requestMessage);
		if (data == null) {
			return Mono.just(requestMessage);
		}
		else {
			return Mono.just(data)
					.map((payload) ->
							MessageBuilder.withPayload(payload)
									.copyHeaders(requestMessage.getHeaders())
									.build());
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private Object decodePayload(Message<?> requestMessage) {
		ResolvableType elementType;
		MimeType mimeType = requestMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE, MimeType.class);
		if (this.requestElementType == null) {
			elementType =
					mimeType != null && "text".equals(mimeType.getType())
							? ResolvableType.forClass(String.class)
							: ResolvableType.forClass(byte[].class);
		}
		else {
			elementType = this.requestElementType;
		}

		Object payload = requestMessage.getPayload();

		// The MessagingRSocket logic ensures that we can have only a single DataBuffer payload or Flux<DataBuffer>.
		Decoder<Object> decoder = this.rsocketStrategies.decoder(elementType, mimeType);
		if (payload instanceof DataBuffer) {
			return decoder.decode((DataBuffer) payload, elementType, mimeType, null);
		}
		else if (this.decodeFluxAsUnit) {
			return decoder.decode((Publisher<DataBuffer>) payload, elementType, mimeType, null);
		}
		else {
			return Flux.from((Publisher<DataBuffer>) payload)
					.handle((buffer, synchronousSink) -> {
						Object value = decoder.decode(buffer, elementType, mimeType, null);
						if (value != null) {
							synchronousSink.next(value);
						}
					});
		}
	}

	private Flux<DataBuffer> createReply(Object reply, Message<?> requestMessage) {
		MessageHeaders requestMessageHeaders = requestMessage.getHeaders();
		DataBufferFactory bufferFactory =
				requestMessageHeaders.get(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER,
						DataBufferFactory.class);

		if (bufferFactory == null) {
			bufferFactory = this.rsocketStrategies.dataBufferFactory();
		}

		MimeType mimeType = requestMessageHeaders.get(MessageHeaders.CONTENT_TYPE, MimeType.class);

		return encodeContent(reply, ResolvableType.forInstance(reply), bufferFactory, mimeType);
	}

	private Flux<DataBuffer> encodeContent(Object content, ResolvableType returnValueType,
			DataBufferFactory bufferFactory, @Nullable MimeType mimeType) {

		ReactiveAdapter adapter =
				this.rsocketStrategies.reactiveAdapterRegistry()
						.getAdapter(returnValueType.resolve(), content);

		Publisher<?> publisher;
		if (adapter != null) {
			publisher = adapter.toPublisher(content);
		}
		else {
			publisher = Flux.just(content);
		}

		return Flux.from((Publisher<?>) publisher)
				.map((value) -> encodeValue(value, bufferFactory, mimeType));
	}

	private DataBuffer encodeValue(Object element, DataBufferFactory bufferFactory, @Nullable MimeType mimeType) {
		ResolvableType elementType = ResolvableType.forInstance(element);
		Encoder<Object> encoder = this.rsocketStrategies.encoder(elementType, mimeType);
		return encoder.encodeValue(element, bufferFactory, elementType, mimeType, null);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static AtomicReference<Object> getReplyToHeader(Message<?> message) {
		Object headerValue = message.getHeaders().get(RSocketPayloadReturnValueHandler.RESPONSE_HEADER);
		Assert.state(headerValue == null || headerValue instanceof AtomicReference, "Expected AtomicReference");
		return (AtomicReference<Object>) headerValue;
	}

}
