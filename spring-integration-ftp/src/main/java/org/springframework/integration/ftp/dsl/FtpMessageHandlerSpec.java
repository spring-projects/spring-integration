/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.RemoteFileTemplate;
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

	@Deprecated
	protected FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate) {
		this.target = new FtpMessageHandler(remoteFileTemplate.getSessionFactory());
	}

	@Deprecated
	protected FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new FtpMessageHandler(remoteFileTemplate, fileExistsMode);
	}

	protected FtpMessageHandlerSpec(FtpRemoteFileTemplate ftpRemoteFileTemplate) {
		this.target = new FtpMessageHandler(ftpRemoteFileTemplate);
	}

	protected FtpMessageHandlerSpec(FtpRemoteFileTemplate ftpRemoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new FtpMessageHandler(ftpRemoteFileTemplate, fileExistsMode);
	}

}
