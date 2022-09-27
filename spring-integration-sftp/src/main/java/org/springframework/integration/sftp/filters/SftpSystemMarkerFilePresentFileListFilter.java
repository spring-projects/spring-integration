/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.sftp.filters;

import java.util.Map;
import java.util.function.Function;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * SFTP implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class SftpSystemMarkerFilePresentFileListFilter
		extends AbstractMarkerFilePresentFileListFilter<SftpClient.DirEntry> {

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter) {
		super(filter);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter, String suffix) {
		super(filter, suffix);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter,
			Function<String, String> function) {

		super(filter, function);
	}

	public SftpSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<SftpClient.DirEntry>, Function<String, String>> filtersAndFunctions) {

		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(SftpClient.DirEntry file) {
		return file.getFilename();
	}

}
