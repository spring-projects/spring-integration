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

import java.net.URI;
import java.util.function.Consumer;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import io.rsocket.util.EmptyPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * A client {@link AbstractRSocketConnector} extension to the RSocket server.
 * <p>
 * Note: the {@link RSocketFactory.ClientRSocketFactory#acceptor(java.util.function.Function)}
 * in the provided {@link #factoryConfigurer} is overridden with an internal {@link IntegrationRSocketAcceptor}
 * for the proper Spring Integration channel adapter mappings.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketFactory.ClientRSocketFactory
 * @see RSocketRequester
 */
public class ClientRSocketConnector extends AbstractRSocketConnector {

	private final ClientTransport clientTransport;

	private Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer = (clientRSocketFactory) -> { };

	private String connectRoute;

	private String connectData = "";

	private boolean autoConnect;

	private Mono<RSocket> rsocketMono;

	/**
	 * Instantiate a connector based on the {@link TcpClientTransport}.
	 * @param host the TCP host to connect.
	 * @param port the TCP port to connect.
	 * @see #ClientRSocketConnector(ClientTransport)
	 */
	public ClientRSocketConnector(String host, int port) {
		this(TcpClientTransport.create(host, port));
	}

	/**
	 * Instantiate a connector based on the {@link WebsocketClientTransport}.
	 * @param uri the WebSocket URI to connect.
	 * @see #ClientRSocketConnector(ClientTransport)
	 */
	public ClientRSocketConnector(URI uri) {
		this(WebsocketClientTransport.create(uri));
	}

	/**
	 * Instantiate a connector based on the provided {@link ClientTransport}.
	 * @param clientTransport the {@link ClientTransport} to use.
	 */
	public ClientRSocketConnector(ClientTransport clientTransport) {
		super(new IntegrationRSocketAcceptor());
		Assert.notNull(clientTransport, "'clientTransport' must not be null");
		this.clientTransport = clientTransport;
	}

	/**
	 * Specify a {@link Consumer} for  configuring a {@link RSocketFactory.ClientRSocketFactory}.
	 * @param factoryConfigurer the {@link Consumer} to configure the {@link RSocketFactory.ClientRSocketFactory}.
	 */
	public void setFactoryConfigurer(Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	/**
	 * Configure a route for server RSocket endpoint.
	 * @param connectRoute the route to connect to.
	 */
	public void setConnectRoute(String connectRoute) {
		this.connectRoute = connectRoute;
	}

	/**
	 * Configure a data for connect.
	 * Defaults to empty string.
	 * @param connectData the data for connect frame.
	 */
	public void setConnectData(String connectData) {
		Assert.notNull(connectData, "'connectData' must not be null");
		this.connectData = connectData;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		RSocketFactory.ClientRSocketFactory clientFactory =
				RSocketFactory.connect()
						.dataMimeType(getDataMimeType().toString());
		this.factoryConfigurer.accept(clientFactory);
		clientFactory.acceptor(this.rsocketAcceptor);
		Payload connectPayload = EmptyPayload.INSTANCE;
		if (this.connectRoute != null) {
			connectPayload = DefaultPayload.create(this.connectData, this.connectRoute);
		}
		clientFactory.setupPayload(connectPayload);
		this.rsocketMono = clientFactory.transport(this.clientTransport).start().cache();
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.autoConnect = this.rsocketAcceptor.detectEndpoints();
	}

	@Override
	protected void doStart() {
		if (this.autoConnect) {
			connect();
		}
	}

	@Override
	public void destroy() {
		this.rsocketMono
				.doOnNext(Disposable::dispose)
				.subscribe();
	}

	/**
	 * Perform subscription into the RSocket server for incoming requests.
	 */
	public void connect() {
		this.rsocketMono.subscribe();
	}

	public Mono<RSocketRequester> getRSocketRequester() {
		return this.rsocketMono
				.map((rsocket) -> RSocketRequester.wrap(rsocket, getDataMimeType(), getRSocketStrategies()))
				.cache();
	}

}
