/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
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
