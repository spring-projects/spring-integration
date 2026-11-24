/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
