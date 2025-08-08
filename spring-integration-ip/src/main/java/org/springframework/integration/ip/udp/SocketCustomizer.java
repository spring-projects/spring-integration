/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
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
