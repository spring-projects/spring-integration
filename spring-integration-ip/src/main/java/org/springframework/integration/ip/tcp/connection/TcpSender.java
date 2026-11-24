/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
