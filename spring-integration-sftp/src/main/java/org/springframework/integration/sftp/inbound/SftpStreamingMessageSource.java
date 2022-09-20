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

package org.springframework.integration.sftp.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.session.SftpFileInfo;

/**
 * Message source for streaming SFTP remote file contents.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class SftpStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<SftpClient.DirEntry> {

	/**
	 * Construct an instance with the supplied template.
	 * @param template the template.
	 */
	public SftpStreamingMessageSource(RemoteFileTemplate<SftpClient.DirEntry> template) {
		this(template, null);
	}

	/**
	 * Construct an instance with the supplied template and comparator.
	 * Note: the comparator is applied each time the remote directory is listed
	 * which only occurs when the previous list is exhausted.
	 * @param template the template.
	 * @param comparator the comparator.
	 */
	public SftpStreamingMessageSource(RemoteFileTemplate<SftpClient.DirEntry> template,
			Comparator<SftpClient.DirEntry> comparator) {

		super(template, comparator);
		doSetFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftpStreamingMessageSource"));
	}

	@Override
	public String getComponentType() {
		return "sftp:inbound-streaming-channel-adapter";
	}

	@Override
	protected List<AbstractFileInfo<SftpClient.DirEntry>> asFileInfoList(Collection<SftpClient.DirEntry> files) {
		List<AbstractFileInfo<SftpClient.DirEntry>> canonicalFiles = new ArrayList<>();
		for (SftpClient.DirEntry file : files) {
			canonicalFiles.add(new SftpFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file != null && file.getAttributes().isDirectory();
	}

}
