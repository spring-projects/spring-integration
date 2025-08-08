/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

/**
 * ApplicationEvent representing exceptions on a {@link TcpConnection}.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionExceptionEvent extends TcpConnectionEvent {

	private static final long serialVersionUID = 1335560439854592185L;

	public TcpConnectionExceptionEvent(TcpConnection connection,
			String connectionFactoryName, Throwable cause) {
		super(connection, connectionFactoryName, cause);
	}

}
