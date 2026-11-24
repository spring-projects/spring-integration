/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
