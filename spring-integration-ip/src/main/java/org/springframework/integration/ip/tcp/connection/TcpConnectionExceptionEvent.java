/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.Serial;

/**
 * ApplicationEvent representing exceptions on a {@link TcpConnection}.
 *
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
public class TcpConnectionExceptionEvent extends TcpConnectionEvent {

	@Serial
	private static final long serialVersionUID = 1335560439854592185L;

	public TcpConnectionExceptionEvent(TcpConnection connection,
			String connectionFactoryName, Throwable cause) {

		super(connection, connectionFactoryName, cause);
	}

}
