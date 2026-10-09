/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.net.SocketAddress;

import org.jspecify.annotations.Nullable;

/**
 * Connection factories that act as TCP servers, listening for incoming connections.
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface TcpServerConnectionFactory {

	/**
	 * Return the port this server is listening on.
	 * If the factory is configured to listen on a random port (0), this
	 * will return the actual port after the factory is started. It may
	 * return the previous value if the factory is stopped.
	 * @return the port.
	 */
	int getPort();

	/**
	 * Return the {@link SocketAddress} that the underlying {@code ServerSocket}
	 * is bound to.
	 * @return the socket address.
	 */
	default @Nullable SocketAddress getServerSocketAddress() {
		return null;
	}

}
