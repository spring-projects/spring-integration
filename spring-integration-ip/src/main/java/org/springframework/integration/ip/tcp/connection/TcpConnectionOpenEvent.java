/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.Serial;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionOpenEvent extends TcpConnectionEvent {

	@Serial
	private static final long serialVersionUID = 7237316997596598287L;

	public TcpConnectionOpenEvent(TcpConnection connection, String connectionFactoryName) {
		super(connection, connectionFactoryName);
	}

	@Override
	public String toString() {
		return super.toString() + " **OPENED**";
	}

}
