/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.Serial;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * An event emitted when a connection could not be established for some
 * reason.
 *
 * @author Gary Russell
 * @since 4.3.2
 *
 */
public class TcpConnectionFailedEvent extends IpIntegrationEvent {

	@Serial
	private static final long serialVersionUID = -7460880274740273542L;

	public TcpConnectionFailedEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
