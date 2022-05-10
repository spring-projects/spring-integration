/*
 * Copyright 2022 the original author or authors.
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
