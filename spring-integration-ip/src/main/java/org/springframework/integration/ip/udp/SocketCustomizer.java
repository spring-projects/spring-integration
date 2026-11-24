/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.udp;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Configures a socket.
 *
 * @author Gary Russell
 * @since 5.3.3
 */
@FunctionalInterface
public interface SocketCustomizer {

	/**
	 * Configure the socket ({code setTrafficClass()}, etc).
	 * @param socket the socket.
	 * @throws SocketException a socket exception.
	 */
	void configure(DatagramSocket socket) throws SocketException;

}
