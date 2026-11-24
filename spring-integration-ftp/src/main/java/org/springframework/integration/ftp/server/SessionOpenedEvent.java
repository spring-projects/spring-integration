/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpSession;

/**
 * An event emitted when a session is opened.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SessionOpenedEvent extends ApacheMinaFtpEvent {

	private static final long serialVersionUID = 1L;

	public SessionOpenedEvent(FtpSession session) {
		super(session);
	}

}
