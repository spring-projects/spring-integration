/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.sftp.inbound;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;

/**
 * Handles the synchronization between a remote SFTP directory and a local mount.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<SftpClient.DirEntry> {

	/**
	 * Create a synchronizer with the {@code SessionFactory} used to acquire {@code Session} instances.
	 * @param sessionFactory The session factory.
	 */
	public SftpInboundFileSynchronizer(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		super(sessionFactory);
		doSetFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftpMessageSource"));
	}

	@Override
	protected boolean isFile(SftpClient.DirEntry file) {
		return file != null && file.getAttributes().isRegularFile();
	}

	@Override
	protected String getFilename(SftpClient.DirEntry file) {
		return file != null ? file.getFilename() : null;
	}

	@Override
	protected long getModified(SftpClient.DirEntry file) {
		return file.getAttributes().getModifyTime().toMillis();
	}

	@Override
	protected String protocol() {
		return "sftp";
	}

}
