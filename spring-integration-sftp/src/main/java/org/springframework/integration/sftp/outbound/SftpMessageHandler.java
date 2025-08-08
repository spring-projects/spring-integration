/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.sftp.outbound;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * Subclass of {@link FileTransferringMessageHandler} for SFTP.
 *
 * @author Gary Russell
 * @author Artme Bilan
 *
 * @since 4.3
 *
 */
public class SftpMessageHandler extends FileTransferringMessageHandler<SftpClient.DirEntry> {

	/**
	 * @param remoteFileTemplate the template.
	 * @see FileTransferringMessageHandler#FileTransferringMessageHandler
	 * (org.springframework.integration.file.remote.RemoteFileTemplate)
	 */
	public SftpMessageHandler(SftpRemoteFileTemplate remoteFileTemplate) {
		super(remoteFileTemplate);
	}

	/**
	 *
	 * @param remoteFileTemplate the template.
	 * @param mode the file exists mode.
	 * @see FileTransferringMessageHandler#FileTransferringMessageHandler
	 * (org.springframework.integration.file.remote.RemoteFileTemplate, FileExistsMode)
	 */
	public SftpMessageHandler(SftpRemoteFileTemplate remoteFileTemplate, FileExistsMode mode) {
		super(remoteFileTemplate, mode);
	}

	/**
	 * @param sessionFactory the session factory.
	 * @see FileTransferringMessageHandler#FileTransferringMessageHandler
	 * (SessionFactory)
	 */
	public SftpMessageHandler(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		this(new SftpRemoteFileTemplate(sessionFactory));
	}

	@Override
	public boolean isChmodCapable() {
		return true;
	}

	@Override
	protected void doChmod(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate, String path, int chmod) {
		remoteFileTemplate.executeWithClient((ClientCallbackWithoutResult<SftpClient>) client -> {
			try {
				SftpClient.Attributes attributes = client.stat(path);
				attributes.setPermissions(chmod);
				client.setStat(path, attributes);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(
						"Failed to execute 'chmod " + Integer.toOctalString(chmod) + " " + path + "'", ex);
			}
		});
	}

}
