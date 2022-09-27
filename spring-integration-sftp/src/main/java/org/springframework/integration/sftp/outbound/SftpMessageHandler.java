/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
