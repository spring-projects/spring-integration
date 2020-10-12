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

package org.springframework.integration.rsocket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.messaging.rsocket.annotation.support.RSocketPayloadReturnValueHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import io.rsocket.Payload;
import io.rsocket.frame.FrameType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The {@link RSocketMessageHandler} extension for Spring Integration needs.
 * <p>
 * This class adds an {@link IntegrationRSocketEndpoint} beans detection and registration functionality.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketMessageHandler
 */
class IntegrationRSocketMessageHandler extends RSocketMessageHandler {

	private static final Method HANDLE_MESSAGE_METHOD =
			ReflectionUtils.findMethod(ReactiveMessageHandler.class, "handleMessage", Message.class);

	protected final boolean messageMappingCompatible; // NOSONAR final

	IntegrationRSocketMessageHandler() {
		this(false);
	}

	IntegrationRSocketMessageHandler(boolean messageMappingCompatible) {
		this.messageMappingCompatible = messageMappingCompatible;
		if (!this.messageMappingCompatible) {
			setHandlerPredicate((clazz) -> false);
		}
	}

	public boolean detectEndpoints() {
		ApplicationContext applicationContext = getApplicationContext();
		boolean endpointsDetected = false;
		if (applicationContext != null && getHandlerMethods().isEmpty()) {
			Collection<IntegrationRSocketEndpoint> endpoints =
					applicationContext.getBeansOfType(IntegrationRSocketEndpoint.class)
							.values();
			for (IntegrationRSocketEndpoint endpoint : endpoints) {
				addEndpoint(endpoint);
				endpointsDetected = true;
			}
		}
		return endpointsDetected;
	}

	public void addEndpoint(IntegrationRSocketEndpoint endpoint) {
		RSocketFrameTypeMessageCondition frameTypeMessageCondition = RSocketFrameTypeMessageCondition.EMPTY_CONDITION;

		RSocketInteractionModel[] interactionModels = endpoint.getInteractionModels();
		if (interactionModels.length > 0) {
			frameTypeMessageCondition =
					new RSocketFrameTypeMessageCondition(
							Arrays.stream(interactionModels)
									.map(RSocketInteractionModel::getFrameType)
									.toArray(FrameType[]::new));
		}
		registerHandlerMethod(endpoint, HANDLE_MESSAGE_METHOD,
				new CompositeMessageCondition(
						frameTypeMessageCondition,
						new DestinationPatternsMessageCondition(endpoint.getPath(), getRouteMatcher()))); // NOSONAR
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		if (this.messageMappingCompatible) {
			// Add argument resolver before parent initializes argument resolution
			getArgumentResolverConfigurer().addCustomResolver(new MessageHandlerMethodArgumentResolver());
			return super.initArgumentResolvers();
		}
		else {
			return Collections.singletonList(new MessageHandlerMethodArgumentResolver());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		HandlerMethodReturnValueHandler integrationRSocketPayloadReturnValueHandler =
				new IntegrationRSocketPayloadReturnValueHandler((List<Encoder<?>>) getEncoders(),
						getReactiveAdapterRegistry());
		if (this.messageMappingCompatible) {
			List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
			handlers.add(integrationRSocketPayloadReturnValueHandler);
			handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
			return handlers;
		}
		else {
			return Collections.singletonList(integrationRSocketPayloadReturnValueHandler);
		}
	}

	protected static final class MessageHandlerMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return Message.class.equals(parameter.getParameterType());
		}

		@Override
		public Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {
			return message;
		}

	}

	protected static final class IntegrationRSocketPayloadReturnValueHandler extends RSocketPayloadReturnValueHandler {

		protected IntegrationRSocketPayloadReturnValueHandler(List<Encoder<?>> encoders,
				ReactiveAdapterRegistry registry) {

			super(encoders, registry);
		}

		@Override public Mono<Void> handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
				Message<?> message) {

			AtomicReference<Flux<Payload>> responseReference = getResponseReference(message);

			if (returnValue == null && responseReference != null) {
				return super.handleReturnValue(responseReference.get(), returnType, message);
			}
			else {
				return super.handleReturnValue(returnValue, returnType, message);
			}
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private static AtomicReference<Flux<Payload>> getResponseReference(Message<?> message) {
			Object headerValue = message.getHeaders().get(RESPONSE_HEADER);
			Assert.state(headerValue == null || headerValue instanceof AtomicReference, "Expected AtomicReference");
			return (AtomicReference<Flux<Payload>>) headerValue;
		}

	}

}
