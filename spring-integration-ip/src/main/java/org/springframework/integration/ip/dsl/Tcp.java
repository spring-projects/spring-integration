/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
