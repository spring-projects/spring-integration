/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

/**
 * An event that is emitted when a file is written.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class FileWrittenEvent extends FtpRequestEvent {

	private static final long serialVersionUID = 1L;

	private final boolean append;

	public FileWrittenEvent(FtpSession source, FtpRequest request, boolean append) {
		super(source, request);
		this.append = append;
	}

	public boolean isAppend() {
		return this.append;
	}

	@Override
	public String toString() {
		return "FileWrittenEvent [append=" + this.append
				+ ", request=" + this.request
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
