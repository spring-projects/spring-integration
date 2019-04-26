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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

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
 * A client connector to the RSocket server.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketFactory.ClientRSocketFactory
 * @see RSocketRequester
 */
public class ClientRSocketConnector implements InitializingBean, DisposableBean {

	private final ClientTransport clientTransport;

	private MimeType dataMimeType = MimeTypeUtils.TEXT_PLAIN;

	private Payload connectPayload = EmptyPayload.INSTANCE;

	private RSocketStrategies rsocketStrategies = RSocketStrategies.builder().build();

	private Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer = (clientRSocketFactory) -> { };

	private Mono<RSocket> rsocketMono;

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

	public void setDataMimeType(MimeType dataMimeType) {
		Assert.notNull(dataMimeType, "'dataMimeType' must not be null");
		this.dataMimeType = dataMimeType;
	}

	public void setFactoryConfigurer(Consumer<RSocketFactory.ClientRSocketFactory> factoryConfigurer) {
		Assert.notNull(factoryConfigurer, "'factoryConfigurer' must not be null");
		this.factoryConfigurer = factoryConfigurer;
	}

	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		Assert.notNull(rsocketStrategies, "'rsocketStrategies' must not be null");
		this.rsocketStrategies = rsocketStrategies;
	}

	public void setConnectRoute(String connectRoute) {
		this.connectPayload = DefaultPayload.create("", connectRoute);
	}

	@Override
	public void afterPropertiesSet() {
		RSocketFactory.ClientRSocketFactory clientFactory =
				RSocketFactory.connect()
						.dataMimeType(this.dataMimeType.toString());
		this.factoryConfigurer.accept(clientFactory);
		clientFactory.setupPayload(this.connectPayload);
		this.rsocketMono = clientFactory.transport(this.clientTransport).start();
	}

	public void connect() {
		this.rsocketMono.cache().subscribe();
	}

	public Mono<RSocketRequester> getRSocketRequester() {
		return this.rsocketMono
				.map(rsocket -> RSocketRequester.create(rsocket, this.dataMimeType, this.rsocketStrategies))
				.cache();
	}

	@Override
	public void destroy() {
		this.rsocketMono
				.doOnNext(Disposable::dispose)
				.subscribe();
	}

}
