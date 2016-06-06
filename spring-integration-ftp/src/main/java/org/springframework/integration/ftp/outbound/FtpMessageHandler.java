/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ftp.outbound;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * The FTP specific {@link FileTransferringMessageHandler} extension.
 * Based on the {@link FtpRemoteFileTemplate}.
 *
 * @author Artem Bilan
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

	public FtpMessageHandler(RemoteFileTemplate<FTPFile> remoteFileTemplate, FileExistsMode mode) {
		super(remoteFileTemplate, mode);
	}

}
