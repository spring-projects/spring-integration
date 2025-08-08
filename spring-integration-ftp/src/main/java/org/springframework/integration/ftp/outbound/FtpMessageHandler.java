/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ftp.outbound;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * The FTP specific {@link FileTransferringMessageHandler} extension. Based on the
 * {@link FtpRemoteFileTemplate}.
 *
 * @author Artem Bilan
 * @author Deepak Gunasekaran
 *
 * @since 4.1.9
 *
 * @see FtpRemoteFileTemplate
 */
public class FtpMessageHandler extends FileTransferringMessageHandler<FTPFile> {

	public FtpMessageHandler(SessionFactory<FTPFile> sessionFactory) {
		this(new FtpRemoteFileTemplate(sessionFactory));
		((FtpRemoteFileTemplate) this.remoteFileTemplate).setExistsMode(FtpRemoteFileTemplate.ExistsMode.NLST);
	}

	public FtpMessageHandler(FtpRemoteFileTemplate remoteFileTemplate) {
		super(remoteFileTemplate);
	}

	/**
	 * Constructor which sets the FtpRemoteFileTemplate and FileExistsMode.
	 * @param ftpRemoteFileTemplate the remote file template.
	 * @param mode the file exists mode.
	 * @since 5.4
	 */
	public FtpMessageHandler(FtpRemoteFileTemplate ftpRemoteFileTemplate, FileExistsMode mode) {
		super(ftpRemoteFileTemplate, mode);
	}

	@Override
	public boolean isChmodCapable() {
		return true;
	}

	@Override
	protected void doChmod(RemoteFileTemplate<FTPFile> remoteFileTemplate, final String path, final int chmod) {
		remoteFileTemplate.executeWithClient((ClientCallbackWithoutResult<FTPClient>) client -> {
			String chModCommand = "chmod " + Integer.toOctalString(chmod) + " " + path;
			try {
				client.sendSiteCommand(chModCommand);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to execute '" + chModCommand + "'", e);
			}
		});
	}

}
