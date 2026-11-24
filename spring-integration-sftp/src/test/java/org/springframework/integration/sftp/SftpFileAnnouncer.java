/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
