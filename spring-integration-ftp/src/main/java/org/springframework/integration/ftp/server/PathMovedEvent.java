/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

/**
 * An event emitted when a path is moved.
 * @author Gary Russell
 *
 * @since 5.2
 *
 */
public class PathMovedEvent extends FtpRequestEvent {

	private static final long serialVersionUID = 1L;

	public PathMovedEvent(FtpSession source, FtpRequest request) {
		super(source, request);
	}

}
