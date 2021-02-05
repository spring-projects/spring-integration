/*
 * Copyright 2013-2021 the original author or authors.
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

import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Persistent file list filter using the server's file timestamp to detect if we've already
 * 'seen' this file.
 *
 * @author Gary Russell
 * @author David Liu
 *
 * @since 3.0
 *
 */
public class SftpPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<LsEntry> {

	public SftpPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(LsEntry file) {
		return ((long) file.getAttrs().getMTime()) * 1000;
	}

	@Override
	protected String fileName(LsEntry file) {
		return file.getFilename();
	}

	@Override
	protected boolean isDirectory(LsEntry file) {
		return file.getAttrs().isDir();
	}

}
