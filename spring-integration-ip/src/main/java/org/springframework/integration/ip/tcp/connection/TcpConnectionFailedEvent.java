/*
 * Copyright 2016-present the original author or authors.
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
