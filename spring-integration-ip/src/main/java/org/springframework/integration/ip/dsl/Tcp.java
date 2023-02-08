/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;

/**
 * Factory methods for TCP.
 *
 * @author Gary Russell
 * @author Tim Ysewyn
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public final class Tcp {

	private Tcp() {
	}

	/**
	 * Create a server spec that uses NIO.
	 * @param port the port to listen on.
	 * @return the spec.
	 */
	public static TcpNioServerConnectionFactorySpec nioServer(int port) {
		return new TcpNioServerConnectionFactorySpec(port);
	}

	/**
	 * Create a server spec that does not use NIO.
	 * @param port the port to listen on.
	 * @return the spec.
	 */
	public static TcpNetServerConnectionFactorySpec netServer(int port) {
		return new TcpNetServerConnectionFactorySpec(port);
	}

	/**
	 * Create a client spec that uses NIO.
	 * @param host the host to connect to.
	 * @param port the port to connect to.
	 * @return the spec.
	 */
	public static TcpNioClientConnectionFactorySpec nioClient(String host, int port) {
		return new TcpNioClientConnectionFactorySpec(host, port);
	}

	/**
	 * Create a client spec that does not use NIO.
	 * @param host the host to connect to.
	 * @param port the port to connect to.
	 * @return the spec.
	 */
	public static TcpNetClientConnectionFactorySpec netClient(String host, int port) {
		return new TcpNetClientConnectionFactorySpec(host, port);
	}

	/**
	 * Create an inbound gateway using the supplied connection factory.
	 * @param connectionFactory the connection factory - must be an existing bean - it
	 * will not be initialized.
	 * @return the spec.
	 */
	public static TcpInboundGatewaySpec inboundGateway(AbstractConnectionFactory connectionFactory) {
		return new TcpInboundGatewaySpec(connectionFactory);
	}

	/**
	 * Create an inbound gateway using the supplied connection factory.
	 * @param connectionFactorySpec the connection factory spec.
	 * @return the spec.
	 */
	public static TcpInboundGatewaySpec inboundGateway(AbstractConnectionFactorySpec<?, ?> connectionFactorySpec) {
		return new TcpInboundGatewaySpec(connectionFactorySpec);
	}

	/**
	 * Create an inbound channel adapter using the supplied connection factory.
	 * @param connectionFactory the connection factory - must be an existing bean - it
	 * will not be initialized.
	 * @return the spec.
	 */
	public static TcpInboundChannelAdapterSpec inboundAdapter(AbstractConnectionFactory connectionFactory) {
		return new TcpInboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * Create an inbound channel adapter using the supplied connection factory.
	 * @param connectionFactorySpec the connection factory spec.
	 * @return the spec.
	 */
	public static TcpInboundChannelAdapterSpec inboundAdapter(
			AbstractConnectionFactorySpec<?, ?> connectionFactorySpec) {

		return new TcpInboundChannelAdapterSpec(connectionFactorySpec);
	}

	/**
	 * Create an outbound gateway using the supplied client connection factory.
	 * @param connectionFactory the connection factory - must be an existing bean - it
	 * will not be initialized.
	 * @return the spec.
	 */
	public static TcpOutboundGatewaySpec outboundGateway(AbstractClientConnectionFactory connectionFactory) {
		return new TcpOutboundGatewaySpec(connectionFactory);
	}

	/**
	 * Create an outbound gateway using the supplied client connection factory.
	 * @param connectionFactory the connection factory spec.
	 * @return the spec.
	 */
	public static TcpOutboundGatewaySpec outboundGateway(TcpClientConnectionFactorySpec<?, ?> connectionFactory) {
		return new TcpOutboundGatewaySpec(connectionFactory);
	}

	/**
	 * Create an outbound gateway using the supplied connection factory.
	 * @param connectionFactory the connection factory - must be an existing bean - it
	 * will not be initialized.
	 * @return the spec.
	 */
	public static TcpOutboundChannelAdapterSpec outboundAdapter(AbstractConnectionFactory connectionFactory) {
		return new TcpOutboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * Create an outbound gateway using the supplied connection factory.
	 * @param connectionFactorySpec the connection factory.
	 * @return the spec.
	 */
	public static TcpOutboundChannelAdapterSpec outboundAdapter(
			AbstractConnectionFactorySpec<?, ?> connectionFactorySpec) {

		return new TcpOutboundChannelAdapterSpec(connectionFactorySpec);
	}

}
