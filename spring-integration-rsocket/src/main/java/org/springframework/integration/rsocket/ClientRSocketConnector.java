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
import java.util.function.Function;

import org.springframework.context.SmartLifecycle;
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
 * Note: the {@link RSocketFactory.ClientRSocketFactory#acceptor(Function)}
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
public class ClientRSocketConnector extends AbstractRSocketConnector implements SmartLifecycle {

	private final ClientTransport clientTransport;

	private Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer = (clientRSocketFactory) -> { };

	private String connectRoute;

	private String connectData = "";

	private Mono<RSocket> rsocketMono;

	private volatile boolean running;

	private boolean autoConnect;

	public ClientRSocketConnector(String host, int port) {
		this(TcpClientTransport.create(host, port));
	}

	public ClientRSocketConnector(URI uri) {
		this(WebsocketClientTransport.create(uri));
	}

	public ClientRSocketConnector(ClientTransport clientTransport) {
		Assert.notNull(clientTransport, "'clientTransport' must not be null");
		this.clientTransport = clientTransport;
	}

	public void setFactoryConfigurer(Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	public void setConnectRoute(String connectRoute) {
		this.connectRoute = connectRoute;
	}

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
	public void destroy() {
		super.destroy();
		this.rsocketMono
				.doOnNext(Disposable::dispose)
				.subscribe();
	}

	@Override
	public void start() {
		if (!this.running) {
			this.running = true;
			if (this.autoConnect) {
				connect();
			}
		}
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Perform subscription into the RSocket server for incoming requests.
	 */
	public void connect() {
		this.rsocketMono.subscribe();
	}

	public Mono<RSocketRequester> getRSocketRequester() {
		return this.rsocketMono
				.map(rsocket -> RSocketRequester.wrap(rsocket, getDataMimeType(), getRSocketStrategies()))
				.cache();
	}

}
