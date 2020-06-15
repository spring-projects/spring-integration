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
	 * Constructor which sets the RemoteFileTemplate and FileExistsMode.
	 * @param remoteFileTemplate the remote file template.
	 * @param mode the file exists mode.
	 * @deprecated in favor of
	 * {@link #FtpMessageHandler(FtpRemoteFileTemplate, FileExistsMode)}
	 */
	@Deprecated
	public FtpMessageHandler(RemoteFileTemplate<FTPFile> remoteFileTemplate, FileExistsMode mode) {
		super(remoteFileTemplate, mode);
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
				throw new UncheckedIOException("Failed to execute '" + chModCommand  + "'", e);
			}
		});
	}

}
