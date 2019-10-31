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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.buffer.DataBuffer;
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

	public ServerRSocketMessageHandler() {
		this(false);
	}

	public ServerRSocketMessageHandler(boolean requestMappingCompatible) {
		super(requestMappingCompatible);
	}


	public Map<Object, RSocketRequester> getClientRSocketRequesters() {
		return Collections.unmodifiableMap(this.clientRSocketRequesters);
	}

	public void setClientRSocketKeyStrategy(
			BiFunction<Map<String, Object>, DataBuffer, Object> clientRSocketKeyStrategy) {

		Assert.notNull(clientRSocketKeyStrategy, "'clientRSocketKeyStrategy' must not be null");
		this.clientRSocketKeyStrategy = clientRSocketKeyStrategy;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	void registerHandleConnectionSetupMethod() {
		registerHandlerMethod(this, HANDLE_CONNECTION_SETUP_METHOD,
				new CompositeMessageCondition(
						RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
						new DestinationPatternsMessageCondition(new String[] { "*" }, obtainRouteMatcher())));
	}

	@SuppressWarnings("unused")
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
