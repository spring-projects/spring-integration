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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import io.rsocket.core.RSocketServer;
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
 * @see io.rsocket.core.RSocketConnector
 */
public class ServerRSocketConnector extends AbstractRSocketConnector implements ApplicationEventPublisherAware {

	private final ServerTransport<CloseableChannel> serverTransport;

	private Consumer<RSocketServer> serverConfigurer = (rsocketServer) -> {
	};

	private Mono<CloseableChannel> serverMono;

	/**
	 * Instantiate a server connector based on a provided {@link ServerRSocketMessageHandler}
	 * with an assumption that RSocket server is created externally as well.
	 * All other options are ignored in favor of provided {@link ServerRSocketMessageHandler}
	 * and its external RSocket server configuration.
	 * @param serverRSocketMessageHandler the {@link ServerRSocketMessageHandler} to rely on.
	 * @since 5.2.1
	 */
	public ServerRSocketConnector(ServerRSocketMessageHandler serverRSocketMessageHandler) {
		super(serverRSocketMessageHandler);
		this.serverTransport = null;
	}

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

	private ServerRSocketMessageHandler serverRSocketMessageHandler() {
		return (ServerRSocketMessageHandler) this.rSocketMessageHandler;
	}

	/**
	 * Provide a {@link Consumer} to configure the {@link RSocketServer}.
	 * @param serverConfigurer the {@link Consumer} to configure the {@link RSocketServer}.
	 * @since 5.2.6
	 */
	public void setServerConfigurer(Consumer<RSocketServer> serverConfigurer) {
		this.serverConfigurer = serverConfigurer;
	}

	/**
	 * Configure a strategy to determine a key for the client {@link RSocketRequester} connected.
	 * Defaults to the {@code destination} to which a client is connected.
	 * @param clientRSocketKeyStrategy the {@link BiFunction} to determine a key for client {@link RSocketRequester}s.
	 */
	public void setClientRSocketKeyStrategy(BiFunction<Map<String, Object>,
			DataBuffer, Object> clientRSocketKeyStrategy) {

		if (this.serverTransport != null) {
			serverRSocketMessageHandler().setClientRSocketKeyStrategy(clientRSocketKeyStrategy);
		}
	}

	@Override
	public void setDataMimeType(@Nullable MimeType dataMimeType) {
		if (this.serverTransport != null) {
			super.setDataMimeType(dataMimeType);
		}
	}

	@Override
	public void setMetadataMimeType(MimeType metadataMimeType) {
		if (this.serverTransport != null) {
			super.setMetadataMimeType(metadataMimeType);
		}
	}

	@Override
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		if (this.serverTransport != null) {
			super.setRSocketStrategies(rsocketStrategies);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (this.serverTransport != null) {
			super.setApplicationContext(applicationContext);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		if (this.serverTransport != null) {
			serverRSocketMessageHandler().setApplicationEventPublisher(applicationEventPublisher);
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.serverTransport != null) {
			super.afterPropertiesSet();
			RSocketServer rsocketServer = RSocketServer.create();
			this.serverConfigurer.accept(rsocketServer);

			this.serverMono =
					rsocketServer
							.acceptor(serverRSocketMessageHandler().responder())
							.bind(this.serverTransport)
							.cache();
		}
	}

	/**
	 * Return connected {@link RSocketRequester}s mapped by keys.
	 * @return connected {@link RSocketRequester}s mapped by keys.
	 * @see ServerRSocketMessageHandler#getClientRSocketRequesters()
	 */
	public Map<Object, RSocketRequester> getClientRSocketRequesters() {
		return serverRSocketMessageHandler().getClientRSocketRequesters();
	}

	/**
	 * Return connected {@link RSocketRequester} mapped by key or null.
	 * @param key the mapping key.
	 * @return the {@link RSocketRequester} or null.
	 * @see ServerRSocketMessageHandler#getClientRSocketRequester(Object)
	 */
	@Nullable
	public RSocketRequester getClientRSocketRequester(Object key) {
		return serverRSocketMessageHandler().getClientRSocketRequester(key);
	}

	/**
	 * Return the port this internal server is bound or empty {@link Mono}.
	 * @return the port this internal server is bound or empty {@link Mono}
	 * if an external server is used.
	 */
	public Mono<Integer> getBoundPort() {
		if (this.serverTransport != null) {
			return this.serverMono
					.map((server) -> server.address().getPort());
		}
		else {
			return Mono.empty();
		}
	}

	@Override
	protected void doStart() {
		if (this.serverTransport != null) {
			this.serverMono.subscribe();
		}
	}

	@Override
	public void destroy() {
		if (this.serverTransport != null) {
			this.serverMono
					.doOnNext(Disposable::dispose)
					.subscribe();
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		super.afterSingletonsInstantiated();
		serverRSocketMessageHandler().registerHandleConnectionSetupMethod();
	}

}
