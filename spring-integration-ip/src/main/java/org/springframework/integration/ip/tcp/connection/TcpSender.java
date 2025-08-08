/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

/**
 * An interface representing a sending client of a connection
 * factory.
 * @author Gary Russell
 * @since 2.0
 *
 */
@FunctionalInterface
public interface TcpSender {

	/**
	 * When we are using sockets owned by a {@link TcpListener}, this
	 * method is called each time a new connection is made.
	 * @param connection The connection.
	 */
	void addNewConnection(TcpConnection connection);

	/**
	 * When we are using sockets owned by a {@link TcpListener}, this
	 * method is called each time a connection is closed.
	 * @param connection The connection.
	 */
	default void removeDeadConnection(TcpConnection connection) {
	}

}
