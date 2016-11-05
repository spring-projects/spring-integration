/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;

/**
 * Factory methods for TCP.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public final class Tcp {

	/**
	 * Boolean indicating the connection factory should use NIO.
	 */
	public static final boolean NIO = true;

	/**
	 * Boolean indicating the connection factory should not use NIO
	 * (default).
	 */
	public static final boolean NET = true;

	private Tcp() {
		super();
	}

	/**
	 * Create a server spec that uses NIO.
	 * @param port the port to listen on.
	 * @param <S> the spec type.
	 * @param <C> the connection factrory type.
	 * @return the spec.
	 */
	public static <S extends TcpServerConnectionFactorySpec<S, C>, C extends AbstractServerConnectionFactory>
			TcpServerConnectionFactorySpec<S, C> nioServer(int port) {
		return new TcpServerConnectionFactorySpec<>(port, NIO);
	}

	/**
	 * Create a server spec that does not use NIO.
	 * @param port the port to listen on.
	 * @param <S> the spec type.
	 * @param <C> the connection factrory type.
	 * @return the spec.
	 */
	public static <S extends TcpServerConnectionFactorySpec<S, C>, C extends AbstractServerConnectionFactory>
			TcpServerConnectionFactorySpec<S, C> netServer(int port) {
		return new TcpServerConnectionFactorySpec<>(port, NET);
	}

	/**
	 * Create a client spec that uses NIO.
	 * @param host the host to connect to.
	 * @param port the port to connect to.
	 * @param <S> the spec type.
	 * @param <C> the connection factrory type.
	 * @return the spec.
	 */
	public static <S extends TcpClientConnectionFactorySpec<S, C>, C extends AbstractClientConnectionFactory>
			TcpClientConnectionFactorySpec<S, C> nioClient(String host, int port) {
		return new TcpClientConnectionFactorySpec<>(host, port, NIO);
	}

	/**
	 * Create a client spec that does not use NIO.
	 * @param host the host to connect to.
	 * @param port the port to connect to.
	 * @param <S> the spec type.
	 * @param <C> the connection factrory type.
	 * @return the spec.
	 */
	public static <S extends TcpClientConnectionFactorySpec<S, C>, C extends AbstractClientConnectionFactory>
			TcpClientConnectionFactorySpec<S, C> netClient(String host, int port) {
		return new TcpClientConnectionFactorySpec<>(host, port, NET);
	}

	public static <S extends TcpInboundGatewaySpec<S>> TcpInboundGatewaySpec<S> inboundGateway(
			AbstractConnectionFactory connectionFactory) {
		return new TcpInboundGatewaySpec<>(connectionFactory);
	}

	public static <S extends TcpInboundChannelAdapterSpec<S>> TcpInboundChannelAdapterSpec<S> inboundAdapter(
			AbstractConnectionFactory connectionFactory) {
		return new TcpInboundChannelAdapterSpec<>(connectionFactory);
	}

	public static TcpOutboundGatewaySpec outboundGateway(AbstractClientConnectionFactory connectionFactory) {
		return new TcpOutboundGatewaySpec(connectionFactory);
	}

	public static TcpOutboundChannelAdapterSpec outboundAdapter(AbstractConnectionFactory connectionFactory) {
		return new TcpOutboundChannelAdapterSpec(connectionFactory);
	}

}
