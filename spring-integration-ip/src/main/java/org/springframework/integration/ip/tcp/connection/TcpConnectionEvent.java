/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * ApplicationEvent representing normal operations on a {@link TcpConnection}.
 * @author Gary Russell
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class TcpConnectionEvent extends IpIntegrationEvent {

	private final String connectionFactoryName;

	public TcpConnectionEvent(TcpConnection connection,
			String connectionFactoryName) {
		super(connection);
		this.connectionFactoryName = connectionFactoryName;
	}

	public TcpConnectionEvent(TcpConnection connection, String connectionFactoryName,
			Throwable cause) {
		super(connection, cause);
		this.connectionFactoryName = connectionFactoryName;
	}

	public String getConnectionId() {
		return ((TcpConnection) this.getSource()).getConnectionId();
	}

	public String getConnectionFactoryName() {
		return this.connectionFactoryName;
	}

	@Override
	public String toString() {
		return super.toString() +
				", [factory=" + this.connectionFactoryName +
				", connectionId=" + this.getConnectionId() + "]";
	}

}
