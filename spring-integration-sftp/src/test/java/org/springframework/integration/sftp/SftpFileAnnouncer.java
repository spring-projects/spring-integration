package org.springframework.integration.sftp;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Josh Long
 */
@Component("sftpAnnouncer")
public class SftpFileAnnouncer {

	@ServiceActivator
	public void announceFile(File file) {
		System.out.println("New file from the remote host has arrived: " + file.getAbsolutePath());
	}

}
