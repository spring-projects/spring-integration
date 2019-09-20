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
import java.util.function.Consumer;

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

import io.rsocket.RSocketFactory;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

/**
 * A server {@link AbstractRSocketConnector} extension to accept and manage client RSocket connections.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketFactory.ServerRSocketFactory
 */
public class ServerRSocketConnector extends AbstractRSocketConnector
		implements ApplicationEventPublisherAware {

	private final ServerTransport<CloseableChannel> serverTransport;

	private Consumer<RSocketFactory.ServerRSocketFactory> factoryConfigurer = (serverRSocketFactory) -> { };

	private Mono<CloseableChannel> serverMono;

	/**
	 * Instantiate a server connector based on the {@link TcpServerTransport}.
	 * @param bindAddress the local address to bind TCP server onto.
	 * @param port the local TCP port to bind.
	 * @see #ServerRSocketConnector(ServerTransport)
	 */
	public ServerRSocketConnector(String bindAddress, int port) {
		this(TcpServerTransport.create(bindAddress, port));
	}

	/**
	 * Instantiate a server connector based on the {@link WebsocketServerTransport}.
	 * @param server the {@link HttpServer} to use.
	 * @see #ServerRSocketConnector(ServerTransport)
	 */
	public ServerRSocketConnector(HttpServer server) {
		this(WebsocketServerTransport.create(server));
	}

	/**
	 * Instantiate a server connector based on the provided {@link ServerTransport}.
	 * @param serverTransport the {@link ServerTransport} to make server based on.
	 */
	public ServerRSocketConnector(ServerTransport<CloseableChannel> serverTransport) {
		super(new ServerRSocketMessageHandler());
		Assert.notNull(serverTransport, "'serverTransport' must not be null");
		this.serverTransport = serverTransport;
	}

	/**
	 * Provide a {@link Consumer} to configure the {@link RSocketFactory.ServerRSocketFactory}.
	 * @param factoryConfigurer the {@link Consumer} to configure the {@link RSocketFactory.ServerRSocketFactory}.
	 */
	public void setFactoryConfigurer(Consumer<RSocketFactory.ServerRSocketFactory> factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	/**
	 * Configure a strategy to determine a key for the client {@link RSocketRequester} connected.
	 * Defaults to the {@code destination} the client is connected.
	 * @param clientRSocketKeyStrategy the {@link BiFunction} to determine a key for client {@link RSocketRequester}s.
	 */
	public void setClientRSocketKeyStrategy(BiFunction<Map<String, Object>,
			DataBuffer, Object> clientRSocketKeyStrategy) {

		Assert.notNull(clientRSocketKeyStrategy, "'clientRSocketKeyStrategy' must not be null");
		serverRSocketMessageHandler().clientRSocketKeyStrategy = clientRSocketKeyStrategy;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		serverRSocketMessageHandler().applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		RSocketFactory.ServerRSocketFactory serverFactory = RSocketFactory.receive();
		this.factoryConfigurer.accept(serverFactory);

		this.serverMono =
				serverFactory
						.acceptor(serverRSocketMessageHandler().responder())
						.transport(this.serverTransport)
						.start()
						.cache();
	}

	public Map<Object, RSocketRequester> getClientRSocketRequesters() {
		return Collections.unmodifiableMap(serverRSocketMessageHandler().clientRSocketRequesters);
	}

	@Nullable
	public RSocketRequester getClientRSocketRequester(Object key) {
		return serverRSocketMessageHandler().clientRSocketRequesters.get(key);
	}

	public Mono<Integer> getBoundPort() {
		return this.serverMono
				.map((server) -> server.address().getPort());
	}

	private ServerRSocketMessageHandler serverRSocketMessageHandler() {
		return (ServerRSocketMessageHandler) this.rSocketMessageHandler;
	}

	@Override
	protected void doStart() {
		this.serverMono.subscribe();
	}

	@Override
	public void destroy() {
		this.serverMono
				.doOnNext(Disposable::dispose)
				.subscribe();
	}

	@Override
	public void afterSingletonsInstantiated() {
		super.afterSingletonsInstantiated();
		serverRSocketMessageHandler().registerHandleConnectionSetupMethod();
	}

	private static class ServerRSocketMessageHandler extends IntegrationRSocketMessageHandler {

		private static final Method HANDLE_CONNECTION_SETUP_METHOD =
				ReflectionUtils.findMethod(ServerRSocketMessageHandler.class, "handleConnectionSetup", Message.class);


		private final Map<Object, RSocketRequester> clientRSocketRequesters = new HashMap<>();

		private BiFunction<Map<String, Object>, DataBuffer, Object> clientRSocketKeyStrategy =
				(headers, data) -> data.toString(StandardCharsets.UTF_8);

		private ApplicationEventPublisher applicationEventPublisher;

		private void registerHandleConnectionSetupMethod() {
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

}
