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
