/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ftp.dsl;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * A {@link FileTransferringMessageHandlerSpec} for FTP.
 *
 * @author Artem Bilan
 * @author Joaquin Santana
 * @author Deepak Gunasekaran
 *
 * @since 5.0
 */
public class FtpMessageHandlerSpec extends FileTransferringMessageHandlerSpec<FTPFile, FtpMessageHandlerSpec> {

	protected FtpMessageHandlerSpec(SessionFactory<FTPFile> sessionFactory) {
		this.target = new FtpMessageHandler(sessionFactory);
	}

	protected FtpMessageHandlerSpec(FtpRemoteFileTemplate ftpRemoteFileTemplate) {
		this.target = new FtpMessageHandler(ftpRemoteFileTemplate);
	}

	protected FtpMessageHandlerSpec(FtpRemoteFileTemplate ftpRemoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new FtpMessageHandler(ftpRemoteFileTemplate, fileExistsMode);
	}

}
