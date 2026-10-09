/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.zeromq;

import org.zeromq.ZMQ;

/**
 * Module that wraps common methods of ZeroMq integration classes
 *
 * @author Alessio Matricardi
 *
 * @since 6.4
 *
 */
public final class ZeroMqUtils {

	/**
	 * Bind the ZeroMq socket to the given port over the TCP transport protocol.
	 * @param socket the ZeroMq socket
	 * @param port the port to bind ZeroMq socket to over TCP. If equal to 0, the socket will bind to a random port.
	 * @return the effectively bound port
	 */
	public static int bindSocket(ZMQ.Socket socket, int port) {
		if (port == 0) {
			return socket.bindToRandomPort("tcp://*");
		}
		else {
			boolean bound = socket.bind("tcp://*:" + port);
			if (!bound) {
				throw new IllegalArgumentException("Cannot bind ZeroMQ socket to port: " + port);
			}
			return port;
		}
	}

	private ZeroMqUtils() {
	}

}
