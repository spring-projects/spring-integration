/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

/**
 * An event emitted when a file or directory is removed.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class PathRemovedEvent extends FtpRequestEvent {

	private static final long serialVersionUID = 1L;

	private final boolean isDirectory;

	public PathRemovedEvent(FtpSession source, FtpRequest request, boolean isDirectory) {
		super(source, request);
		this.isDirectory = isDirectory;
	}

	public boolean isDirectory() {
		return this.isDirectory;
	}

	@Override
	public String toString() {
		return "PathRemovedEvent [isDirectory=" + this.isDirectory
				+ ", request=" + this.request
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
