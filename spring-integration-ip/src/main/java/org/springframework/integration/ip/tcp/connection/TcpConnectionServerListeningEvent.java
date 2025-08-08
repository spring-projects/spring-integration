/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * {@link IpIntegrationEvent} emitted when a server begins listening. Useful
 * when the configured port is zero and the operating system chooses the port.
 * Also useful to avoid polling the {@code isListening()} if you need to wait
 * before starting some other process to connect to the socket.
 *
 * @author Gary Russell
 * @since 4.3
 */
@SuppressWarnings("serial")
public class TcpConnectionServerListeningEvent extends IpIntegrationEvent {

	private final int port;

	public TcpConnectionServerListeningEvent(TcpServerConnectionFactory connectionFactory, int port) {
		super(connectionFactory);
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

}
