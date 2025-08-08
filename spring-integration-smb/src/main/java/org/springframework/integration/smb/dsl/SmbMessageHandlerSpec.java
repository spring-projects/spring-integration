/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.dsl;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.smb.outbound.SmbMessageHandler;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * A {@link FileTransferringMessageHandlerSpec} for SMB.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbMessageHandlerSpec extends FileTransferringMessageHandlerSpec<SmbFile, SmbMessageHandlerSpec> {

	protected SmbMessageHandlerSpec(SessionFactory<SmbFile> sessionFactory) {
		this.target = new SmbMessageHandler(sessionFactory);
	}

	protected SmbMessageHandlerSpec(SmbRemoteFileTemplate smbRemoteFileTemplate) {
		this.target = new SmbMessageHandler(smbRemoteFileTemplate);
	}

	protected SmbMessageHandlerSpec(SmbRemoteFileTemplate smbRemoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new SmbMessageHandler(smbRemoteFileTemplate, fileExistsMode);
	}

}
