/*
 * Copyright 2019 the original author or authors.
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
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.rsocket.RSocketMessageHandler;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.ReflectionUtils;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

/**
 * The {@link RSocketMessageHandler} extension for Spring Integration needs.
 * <p>
 * The most of logic is copied from {@link org.springframework.messaging.rsocket.MessageHandlerAcceptor}.
 * That cannot be extended because it is {@link final}.
 * <p>
 * This class adds an {@link IntegrationRSocketEndpoint} beans detection and registration functionality,
 * as well as serves as a container over an internal {@link IntegrationRSocket} implementation.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see org.springframework.messaging.rsocket.MessageHandlerAcceptor
 */
class IntegrationRSocketAcceptor extends RSocketMessageHandler
		implements SocketAcceptor, Function<RSocket, RSocket>, ApplicationEventPublisherAware {

	private static final Method HANDLE_MESSAGE_METHOD =
			ReflectionUtils.findMethod(ReactiveMessageHandler.class, "handleMessage", Message.class);


	@Nullable
	private MimeType defaultDataMimeType;

	private ApplicationEventPublisher applicationEventPublisher;


	/**
	 * Configure the default content type to use for data payloads.
	 * <p>By default this is not set. However a server acceptor will use the
	 * content type from the {@link ConnectionSetupPayload}, so this is typically
	 * required for clients but can also be used on servers as a fallback.
	 * @param defaultDataMimeType the MimeType to use
	 */
	public void setDefaultDataMimeType(@Nullable MimeType defaultDataMimeType) {
		this.defaultDataMimeType = defaultDataMimeType;
	}


	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public boolean detectEndpoints() {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null && getHandlerMethods().isEmpty()) {
			return applicationContext
					.getBeansOfType(IntegrationRSocketEndpoint.class)
					.values()
					.stream()
					.peek(this::addEndpoint)
					.findAny()
					.isPresent();
		}
		else {
			return false;
		}
	}

	public void addEndpoint(IntegrationRSocketEndpoint endpoint) {
		registerHandlerMethod(endpoint, HANDLE_MESSAGE_METHOD,
				new CompositeMessageCondition(
						new DestinationPatternsMessageCondition(endpoint.getPath(), getPathMatcher())));
	}

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket sendingRSocket) {
		IntegrationRSocket rsocket = createRSocket(sendingRSocket);
		rsocket.setApplicationEventPublisher(this.applicationEventPublisher);

		// Allow handling of the ConnectionSetupPayload via @MessageMapping methods.
		// However, if the handling is to make requests to the client, it's expected
		// it will do so decoupled from the handling, e.g. via .subscribe().
		return rsocket.handleConnectionSetupPayload(setupPayload).then(Mono.just(rsocket));
	}

	@Override
	public RSocket apply(RSocket sendingRSocket) {
		return createRSocket(sendingRSocket);
	}

	private IntegrationRSocket createRSocket(RSocket rsocket) {
		RSocketStrategies rSocketStrategies = getRSocketStrategies();
		return new IntegrationRSocket(this::handleMessage,
				RSocketRequester.wrap(rsocket, this.defaultDataMimeType, rSocketStrategies),
				this.defaultDataMimeType,
				rSocketStrategies.dataBufferFactory());
	}

}
