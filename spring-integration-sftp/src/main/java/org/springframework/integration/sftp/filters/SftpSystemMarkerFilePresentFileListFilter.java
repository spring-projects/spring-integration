/*
 * Copyright 2017-2023 the original author or authors.
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

import com.jcraft.jsch.ChannelSftp.LsEntry;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * SFTP implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class SftpSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<LsEntry> {

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<LsEntry> filter) {
		super(filter);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<LsEntry> filter, String suffix) {
		super(filter, suffix);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<LsEntry> filter,
			Function<String, String> function) {
		super(filter, function);
	}

	public SftpSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<LsEntry>, Function<String, String>> filtersAndFunctions) {
		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(LsEntry file) {
		return file.getFilename();
	}

}
