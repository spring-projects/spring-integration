/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
