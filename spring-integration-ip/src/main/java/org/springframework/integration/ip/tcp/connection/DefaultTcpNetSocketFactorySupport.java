/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
