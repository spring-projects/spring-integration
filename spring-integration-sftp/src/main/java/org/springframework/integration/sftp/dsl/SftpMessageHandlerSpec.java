/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.sftp.dsl;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * @author Artem Bilan
 * @author Joaquin Santana
 * @author Deepak Gunasekaran
 *
 * @since 5.0
 */
public class SftpMessageHandlerSpec
		extends FileTransferringMessageHandlerSpec<SftpClient.DirEntry, SftpMessageHandlerSpec> {

	protected SftpMessageHandlerSpec(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		this.target = new SftpMessageHandler(sessionFactory);
	}

	protected SftpMessageHandlerSpec(SftpRemoteFileTemplate sftpRemoteFileTemplate) {
		this.target = new SftpMessageHandler(sftpRemoteFileTemplate);
	}

	protected SftpMessageHandlerSpec(SftpRemoteFileTemplate sftpRemoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new SftpMessageHandler(sftpRemoteFileTemplate, fileExistsMode);
	}

}
