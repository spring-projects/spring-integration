/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.nio.channels.SocketChannel;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Used by NIO connection factories to instantiate a {@link TcpNioConnection} object.
 * Implementations for SSL and non-SSL {@link TcpNioConnection}s are provided.
 *
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
@FunctionalInterface
public interface TcpNioConnectionSupport {

	/**
	 * Create a new {@link TcpNioConnection} object wrapping the {@link SocketChannel}.
	 * @param socketChannel the SocketChannel.
	 * @param server true if this connection is a server connection.
	 * @param lookupHost true if hostname lookup should be performed, otherwise the connection will
	 * be identified using the ip address.
	 * @param applicationEventPublisher the publisher to which OPEN, CLOSE and EXCEPTION events will
	 * be sent; may be null if event publishing is not required.
	 * @param connectionFactoryName the name of the connection factory creating this connection; used
	 * during event publishing, may be null, in which case "unknown" will be used.
	 * @return the TcpNioConnection
	 */
	TcpNioConnection createNewConnection(SocketChannel socketChannel,
			boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher,
			String connectionFactoryName);

}
