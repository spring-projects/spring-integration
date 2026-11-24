/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
