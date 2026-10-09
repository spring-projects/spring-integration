/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.Serial;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.messaging.MessagingException;

/**
 * An event emitted when an endpoint cannot correlate a connection id to a
 * connection; the cause is a messaging exception with the failed message.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class TcpConnectionFailedCorrelationEvent extends IpIntegrationEvent {

	@Serial
	private static final long serialVersionUID = -7460880274740273542L;

	private final @Nullable String connectionId;

	public TcpConnectionFailedCorrelationEvent(Object source, @Nullable String connectionId, MessagingException cause) {
		super(source, cause);
		this.connectionId = connectionId;
	}

	public @Nullable String getConnectionId() {
		return this.connectionId;
	}

	@Override
	public String toString() {
		return super.toString() +
				", [connectionId=" + this.connectionId + "]";
	}

}
