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

package org.springframework.integration.sftp.dsl;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import com.jcraft.jsch.ChannelSftp;

/**
 * @author Artem Bilan
 * @author Joaquin Santana
 * @author Deepak Gunasekaran
 *
 * @since 5.0
 */
public class SftpMessageHandlerSpec
		extends FileTransferringMessageHandlerSpec<ChannelSftp.LsEntry, SftpMessageHandlerSpec> {

	protected SftpMessageHandlerSpec(SessionFactory<ChannelSftp.LsEntry> sessionFactory) {
		this.target = new SftpMessageHandler(sessionFactory);
	}

	@Deprecated
	protected SftpMessageHandlerSpec(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate) {
		this.target = new SftpMessageHandler(remoteFileTemplate.getSessionFactory());
	}

	@Deprecated
	protected SftpMessageHandlerSpec(RemoteFileTemplate<ChannelSftp.LsEntry> remoteFileTemplate,
			FileExistsMode fileExistsMode) {

		this.target =
				new SftpMessageHandler(new SftpRemoteFileTemplate(remoteFileTemplate.getSessionFactory()),
						fileExistsMode);
	}

	protected SftpMessageHandlerSpec(SftpRemoteFileTemplate sftpRemoteFileTemplate) {
		this.target = new SftpMessageHandler(sftpRemoteFileTemplate);
	}

	protected SftpMessageHandlerSpec(SftpRemoteFileTemplate sftpRemoteFileTemplate, FileExistsMode fileExistsMode) {

		this.target = new SftpMessageHandler(sftpRemoteFileTemplate, fileExistsMode);
	}

}
