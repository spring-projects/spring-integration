/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp.connection;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Strategy interface for modifying sockets.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface TcpSocketSupport {

	/**
	 * Performs any further modifications to the server socket after the connection
	 * factory has created the socket and set any configured attributes, before invoking
	 * {@link ServerSocket#accept()}.
	 * @param serverSocket The ServerSocket
	 */
	void postProcessServerSocket(ServerSocket serverSocket);

	/**
	 * Performs any further modifications to the {@link Socket} after the socket has been
	 * created by a client, or accepted by a server, and after any configured atributes
	 * have been set.
	 * @param socket The Socket
	 */
	void postProcessSocket(Socket socket);

}
