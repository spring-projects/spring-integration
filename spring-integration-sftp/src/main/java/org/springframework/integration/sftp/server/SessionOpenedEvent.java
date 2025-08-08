/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import org.apache.sshd.server.session.ServerSession;

/**
 * An event emitted when a session is opened.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SessionOpenedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private final int clientVersion;

	public SessionOpenedEvent(ServerSession session, int version) {
		super(session);
		this.clientVersion = version;
	}

	public int getClientVersion() {
		return this.clientVersion;
	}

	@Override
	public String toString() {
		return "SessionOpenedEvent [clientVersion=" + this.clientVersion + ", clientAddress="
				+ getSession().getClientAddress() + "]";
	}

}
