/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionCloseEvent extends TcpConnectionEvent {

	private static final long serialVersionUID = 7237316997596598287L;

	public TcpConnectionCloseEvent(TcpConnection connection, String connectionFactoryName) {
		super(connection, connectionFactoryName);
	}

	@Override
	public String toString() {
		return super.toString() + " **CLOSED**";
	}

}
