/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * @author Josh Long
 * @author Gary Russell
 */
@Component("sftpAnnouncer")
public class SftpFileAnnouncer {

	private final Log logger = LogFactory.getLog(getClass());

	@ServiceActivator
	public void announceFile(File file) {
		logger.info("New file from the remote host has arrived: " + file.getAbsolutePath());
	}

}
