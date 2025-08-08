/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Implementation of TcpSocketFactorySupport
 * for non-SSL sockets {@link java.net.ServerSocket} and
 * {@link java.net.Socket}.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTcpNetSocketFactorySupport implements TcpSocketFactorySupport {

	public ServerSocketFactory getServerSocketFactory() {
		return ServerSocketFactory.getDefault();
	}

	public SocketFactory getSocketFactory() {
		return SocketFactory.getDefault();
	}

}
