/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import org.apache.sshd.server.session.ServerSession;

/**
 * An event emitted when a session is closed.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SessionClosedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	public SessionClosedEvent(ServerSession session) {
		super(session);
	}

	@Override
	public String toString() {
		return "SessionClosedEvent [clientAddress=" + getSession().getClientAddress() + "]";
	}

}
