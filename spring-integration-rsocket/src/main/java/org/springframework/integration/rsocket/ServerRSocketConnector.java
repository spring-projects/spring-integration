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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import io.rsocket.Closeable;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

/**
 * A server {@link AbstractRSocketConnector} extension to accept and manage client RSocket connections.
 * <p>
 * Note: the {@link RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor)}
 * in the provided {@link #factoryConfigurer} is overridden with an internal {@link IntegrationRSocketAcceptor}
 * for the proper Spring Integration channel adapter mappings.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketFactory.ServerRSocketFactory
 */
public class ServerRSocketConnector extends AbstractRSocketConnector
		implements ApplicationEventPublisherAware {

	private final ServerTransport<? extends Closeable> serverTransport;

	private Consumer<RSocketFactory.ServerRSocketFactory> factoryConfigurer = (serverRSocketFactory) -> { };

	private Mono<? extends Closeable> serverMono;

	public ServerRSocketConnector(String bindAddress, int port) {
		this(TcpServerTransport.create(bindAddress, port));
	}

	public ServerRSocketConnector(HttpServer server) {
		this(WebsocketServerTransport.create(server));
	}

	public ServerRSocketConnector(ServerTransport<? extends Closeable> serverTransport) {
		super(new ServerRSocketAcceptor());
		Assert.notNull(serverTransport, "'serverTransport' must not be null");
		this.serverTransport = serverTransport;
	}

	public void setFactoryConfigurer(Consumer<RSocketFactory.ServerRSocketFactory> factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	public void setClientRSocketKeyStrategy(BiFunction<String, DataBuffer, Object> clientRSocketKeyStrategy) {
		Assert.notNull(clientRSocketKeyStrategy, "'clientRSocketKeyStrategy' must not be null");
		serverRSocketAcceptor().clientRSocketKeyStrategy = clientRSocketKeyStrategy;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		serverRSocketAcceptor().applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		RSocketFactory.ServerRSocketFactory serverFactory = RSocketFactory.receive();
		this.factoryConfigurer.accept(serverFactory);
		this.serverMono =
				serverFactory
						.acceptor(serverRSocketAcceptor())
						.transport(this.serverTransport)
						.start()
						.cache();
	}

	public Map<Object, RSocketRequester> getClientRSocketRequesters() {
		return Collections.unmodifiableMap(serverRSocketAcceptor().clientRSocketRequesters);
	}

	@Nullable
	public RSocketRequester getClientRSocketRequester(Object key) {
		return serverRSocketAcceptor().clientRSocketRequesters.get(key);
	}

	private ServerRSocketAcceptor serverRSocketAcceptor() {
		return (ServerRSocketAcceptor) this.rsocketAcceptor;
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

	private static class ServerRSocketAcceptor extends IntegrationRSocketAcceptor implements SocketAcceptor {

		private static final Log LOGGER = LogFactory.getLog(IntegrationRSocket.class);

		private final Map<Object, RSocketRequester> clientRSocketRequesters = new HashMap<>();

		private BiFunction<String, DataBuffer, Object> clientRSocketKeyStrategy = (destination, data) -> destination;

		private ApplicationEventPublisher applicationEventPublisher;

		@Override
		public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket sendingRSocket) {
			setupPayload.retain();
			String destination = IntegrationRSocket.getDestination(setupPayload);
			DataBuffer dataBuffer = IntegrationRSocket.payloadToDataBuffer(setupPayload,
					getRSocketStrategies().dataBufferFactory());
			int refCount = IntegrationRSocket.refCount(dataBuffer);
			return Mono.just(sendingRSocket)
					.map(this::createRSocket)
					.doOnNext((rsocket) -> {
						if (StringUtils.hasText(setupPayload.dataMimeType())) {
							rsocket.setDataMimeType(MimeTypeUtils.parseMimeType(setupPayload.dataMimeType()));
						}
						Object rsocketRequesterKey = this.clientRSocketKeyStrategy.apply(destination, dataBuffer);
						this.clientRSocketRequesters.put(rsocketRequesterKey, rsocket.getRequester());
						RSocketConnectedEvent rSocketConnectedEvent =
								new RSocketConnectedEvent(rsocket, destination, dataBuffer, rsocket.getRequester());
						if (this.applicationEventPublisher != null) {
							this.applicationEventPublisher.publishEvent(rSocketConnectedEvent);
						}
						else {
							if (LOGGER.isInfoEnabled()) {
								LOGGER.info("The RSocket has been connected: " + rSocketConnectedEvent);
							}
						}
					})
					.cast(RSocket.class)
					.doFinally((signal) -> {
						if (IntegrationRSocket.refCount(dataBuffer) == refCount) {
							DataBufferUtils.release(dataBuffer);
						}
					});
		}

	}

}
