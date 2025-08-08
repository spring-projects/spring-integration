/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.outbound;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * The SMB specific {@link FileTransferringMessageHandler} extension. Based on the
 * {@link SmbRemoteFileTemplate}.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see SmbRemoteFileTemplate
 */
public class SmbMessageHandler extends FileTransferringMessageHandler<SmbFile> {

	public SmbMessageHandler(SessionFactory<SmbFile> sessionFactory) {
		this(new SmbRemoteFileTemplate(sessionFactory));
	}

	public SmbMessageHandler(SmbRemoteFileTemplate remoteFileTemplate) {
		super(remoteFileTemplate);
	}

	public SmbMessageHandler(SmbRemoteFileTemplate remoteFileTemplate, FileExistsMode mode) {
		super(remoteFileTemplate, mode);
	}

}
