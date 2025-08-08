/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.messaging.MessagingException;

/**
 * An event emitted when an endpoint cannot correlate a connection id to a
 * connection; the cause is a messaging exception with the failed message.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class TcpConnectionFailedCorrelationEvent extends IpIntegrationEvent {

	private static final long serialVersionUID = -7460880274740273542L;

	private final String connectionId;

	public TcpConnectionFailedCorrelationEvent(Object source, String connectionId, MessagingException cause) {
		super(source, cause);
		this.connectionId = connectionId;
	}

	public String getConnectionId() {
		return this.connectionId;
	}

	@Override
	public String toString() {
		return super.toString() +
				", [connectionId=" + this.connectionId + "]";
	}

}
