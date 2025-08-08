/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.ip.udp;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * {@link IpIntegrationEvent} emitted when a server begins listening. Useful
 * when the configured port is zero and the operating system chooses the port.
 * Also useful to avoid polling the {@code isListening()} if you need to wait
 * before starting some other process to connect to the socket.
 *
 * @author Gary Russell
 * @since 5.0.2
 */
@SuppressWarnings("serial")
public class UdpServerListeningEvent extends IpIntegrationEvent {

	private final int port;

	public UdpServerListeningEvent(Object adapter, int port) {
		super(adapter);
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

}
