/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.util.Assert;

/**
 * {@link IpIntegrationEvent} representing exceptions on a TCP server socket/channel.
 *
 * @author Gary Russell
 * @since 4.0.7
 */
@SuppressWarnings("serial")
public class TcpConnectionServerExceptionEvent extends IpIntegrationEvent {

	public TcpConnectionServerExceptionEvent(Object connectionFactory, Throwable cause) {
		super(connectionFactory, cause);
		Assert.notNull(cause, "'cause' cannot be null");
	}

}
