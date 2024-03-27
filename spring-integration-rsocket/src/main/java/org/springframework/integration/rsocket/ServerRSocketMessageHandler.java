/*
 * Copyright 2019-2024 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketRequesterMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * An {@link IntegrationRSocketMessageHandler} extension for RSocket service side.
 * <p>
 * In a plain Spring Integration application instances of this class are created by the
 * {@link ServerRSocketConnector} internally and a new RSocket server is started over there.
 * When an existing RSocket server is in use, an instance of this class has to be
 * provided as a {@link #responder()} into that server and a {@link ServerRSocketConnector}
 * should accept the same instance as a delegate.
 *<p>
 * With a {@link #messageMappingCompatible} option this class also handles
 * {@link org.springframework.messaging.handler.annotation.MessageMapping} methods,
 * covering both Spring Integration and standard
 * {@link org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler}
 * functionality.
 *
 * @author Artem Bilan
 *
 * @since 5.2.1
 */
public class ServerRSocketMessageHandler extends IntegrationRSocketMessageHandler
		implements ApplicationEventPublisherAware {

	private static final Method HANDLE_CONNECTION_SETUP_METHOD =
			ReflectionUtils.findMethod(ServerRSocketMessageHandler.class, "handleConnectionSetup", Message.class);

	private final Map<Object, RSocketRequester> clientRSocketRequesters = new HashMap<>();

	private BiFunction<Map<String, Object>, DataBuffer, Object> clientRSocketKeyStrategy =
			(headers, data) -> data.toString(StandardCharsets.UTF_8);

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Create an service side RSocket message handler instance for delegating
	 * to {@link IntegrationRSocketEndpoint} beans and collect {@link RSocketRequester}s
	 * from client connections.
	 */
	public ServerRSocketMessageHandler() {
		this(false);
	}

	/**
	 * Create an service side RSocket message handler instance for delegating
	 * to {@link IntegrationRSocketEndpoint} beans and collect {@link RSocketRequester}s
	 * from client connections.
	 * When {@code messageMappingCompatible == true}, this class also handles
	 * {@link org.springframework.messaging.handler.annotation.MessageMapping} methods
	 * as it is done by the standard
	 * {@link org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler}.
	 * @param messageMappingCompatible whether handle also
	 * {@link org.springframework.messaging.handler.annotation.MessageMapping}.
	 */
	public ServerRSocketMessageHandler(boolean messageMappingCompatible) {
		super(messageMappingCompatible);
	}

	/**
	 * Configure a {@link BiFunction} to extract a key for mapping connected {@link RSocketRequester}s.
	 * Defaults to the {@code destination} a client is connected.
	 * @param clientRSocketKeyStrategy the {@link BiFunction} to use.
	 */
	public void setClientRSocketKeyStrategy(
			BiFunction<Map<String, Object>, DataBuffer, Object> clientRSocketKeyStrategy) {

		Assert.notNull(clientRSocketKeyStrategy, "'clientRSocketKeyStrategy' must not be null");
		this.clientRSocketKeyStrategy = clientRSocketKeyStrategy;
	}

	/**
	 * Get connected {@link RSocketRequester}s mapped by the keys from the connect messages.
	 * @return the map of connected {@link RSocketRequester}s.
	 * @see #setClientRSocketKeyStrategy
	 */
	public Map<Object, RSocketRequester> getClientRSocketRequesters() {
		return Collections.unmodifiableMap(this.clientRSocketRequesters);
	}

	/**
	 * Obtain a connected {@link RSocketRequester} mapped by provided key or null.
	 * @param key the key for mapped {@link RSocketRequester} if any.
	 * @return the mapped {@link RSocketRequester} or null.
	 */
	@Nullable
	public RSocketRequester getClientRSocketRequester(Object key) {
		return this.clientRSocketRequesters.get(key);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	void registerHandleConnectionSetupMethod() {
		registerHandlerMethod(this, HANDLE_CONNECTION_SETUP_METHOD,
				new CompositeMessageCondition(
						RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
						new DestinationPatternsMessageCondition(new String[] {"*"}, obtainRouteMatcher())));
	}

	@SuppressWarnings("unused")
	@Reflective
	private void handleConnectionSetup(Message<DataBuffer> connectMessage) {
		DataBuffer dataBuffer = connectMessage.getPayload();
		MessageHeaders messageHeaders = connectMessage.getHeaders();
		Object rsocketRequesterKey = this.clientRSocketKeyStrategy.apply(messageHeaders, dataBuffer);
		RSocketRequester rsocketRequester =
				messageHeaders.get(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER,
						RSocketRequester.class);
		this.clientRSocketRequesters.put(rsocketRequesterKey, rsocketRequester);
		RSocketConnectedEvent rSocketConnectedEvent =
				new RSocketConnectedEvent(this, messageHeaders, dataBuffer, rsocketRequester); // NOSONAR
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(rSocketConnectedEvent);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("The RSocket has been connected: " + rSocketConnectedEvent);
			}
		}
	}

}
