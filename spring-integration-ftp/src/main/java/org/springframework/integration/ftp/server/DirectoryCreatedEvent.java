/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

/**
 * An event emitted when a directory is created.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class DirectoryCreatedEvent extends FtpRequestEvent {

	private static final long serialVersionUID = 1L;

	public DirectoryCreatedEvent(FtpSession source, FtpRequest request) {
		super(source, request);
	}

}
