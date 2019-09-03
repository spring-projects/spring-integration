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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.rsocket.ClientRSocketFactoryConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * A client {@link AbstractRSocketConnector} extension to the RSocket connection.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see io.rsocket.RSocketFactory.ClientRSocketFactory
 * @see RSocketRequester
 */
public class ClientRSocketConnector extends AbstractRSocketConnector {

	private final ClientTransport clientTransport;

	private final Map<Object, MimeType> setupMetadata = new LinkedHashMap<>(4);

	private ClientRSocketFactoryConfigurer factoryConfigurer = (clientRSocketFactory) -> { };

	private Object setupData;

	private String setupRoute;

	private Object[] setupRouteVars = new Object[0];

	private boolean autoConnect;

	private Mono<RSocketRequester> rsocketRequesterMono;

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
	 * @see RSocketRequester.Builder#connect(ClientTransport)
	 */
	public ClientRSocketConnector(ClientTransport clientTransport) {
		super(new IntegrationRSocketMessageHandler());
		Assert.notNull(clientTransport, "'clientTransport' must not be null");
		this.clientTransport = clientTransport;
	}

	/**
	 * Callback to configure the {@code ClientRSocketFactory} directly.
	 * Note: this class adds extra {@link ClientRSocketFactoryConfigurer} to the
	 * target {@link RSocketRequester} to populate a reference to an internal
	 * {@link IntegrationRSocketMessageHandler#responder()}.
	 * This overrides possible external
	 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(io.rsocket.SocketAcceptor)}
	 * @param factoryConfigurer the {@link ClientRSocketFactoryConfigurer} to
	 *  configure the {@link io.rsocket.RSocketFactory.ClientRSocketFactory}.
	 * @see RSocketRequester.Builder#rsocketFactory(ClientRSocketFactoryConfigurer)
	 */
	public void setFactoryConfigurer(ClientRSocketFactoryConfigurer factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	/**
	 * Set the route for the setup payload.
	 * @param setupRoute the route to connect to.
	 * @see RSocketRequester.Builder#setupRoute(String, Object...)
	 */
	public void setSetupRoute(String setupRoute) {
		Assert.notNull(setupRoute, "'setupRoute' must not be null");
		this.setupRoute = setupRoute;
	}

	/**
	 * Set the variables for route template to expand with.
	 * @param setupRouteVars the route to connect to.
	 * @see RSocketRequester.Builder#setupRoute(String, Object...)
	 */
	public void setSetupRouteVariables(Object... setupRouteVars) {
		Assert.notNull(setupRouteVars, "'setupRouteVars' must not be null");
		this.setupRouteVars = Arrays.copyOf(setupRouteVars, setupRouteVars.length);
	}

	/**
	 * Add metadata to the setup payload. Composite metadata must be
	 * in use if this is called more than once or in addition to
	 * {@link #setSetupRoute(String)}.
	 * @param setupMetadata the map of metadata to use.
	 * @see RSocketRequester.Builder#setupMetadata(Object, MimeType)
	 */
	public void setSetupMetadata(Map<Object, MimeType> setupMetadata) {
		Assert.notNull(setupMetadata, "'setupMetadata' must not be null");
		this.setupMetadata.clear();
		this.setupMetadata.putAll(setupMetadata);
	}

	/**
	 * Set the data for the setup payload.
	 * @param setupData the data for connect frame.
	 * @see RSocketRequester.Builder#setupData(Object)
	 */
	public void setSetupData(Object setupData) {
		Assert.notNull(setupData, "'setupData' must not be null");
		this.setupData = setupData;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		this.rsocketRequesterMono =
				RSocketRequester.builder()
						.dataMimeType(getDataMimeType())
						.metadataMimeType(getMetadataMimeType())
						.rsocketStrategies(getRSocketStrategies())
						.setupData(this.setupData)
						.setupRoute(this.setupRoute, this.setupRouteVars)
						.rsocketFactory(this.factoryConfigurer)
						.rsocketFactory((rsocketFactory) ->
								rsocketFactory.acceptor(this.rSocketMessageHandler.responder()))
						.apply((builder) -> this.setupMetadata.forEach(builder::setupMetadata))
						.connect(this.clientTransport)
						.cache();
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.autoConnect = this.rSocketMessageHandler.detectEndpoints();
	}

	@Override
	protected void doStart() {
		if (this.autoConnect) {
			connect();
		}
	}

	@Override
	public void destroy() {
		this.rsocketRequesterMono
				.map(RSocketRequester::rsocket)
				.doOnNext(Disposable::dispose)
				.subscribe();
	}

	/**
	 * Perform subscription into the RSocket server for incoming requests.
	 */
	public void connect() {
		this.rsocketRequesterMono.subscribe();
	}

	public Mono<RSocketRequester> getRSocketRequester() {
		return this.rsocketRequesterMono;
	}

}
