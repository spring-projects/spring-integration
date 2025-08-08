/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Strategy interface for supplying Socket Factories.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface TcpSocketFactorySupport {

	/**
	 * Supplies the {@link ServerSocketFactory} to be used to create new
	 * {@link java.net.ServerSocket}s.
	 * @return the ServerSocketFactory
	 */
	ServerSocketFactory getServerSocketFactory();

	/**
	 * Supplies the {@link SocketFactory} to be used to create new
	 * {@link java.net.Socket}s.
	 * @return the SocketFactory
	 */
	SocketFactory getSocketFactory();

}
