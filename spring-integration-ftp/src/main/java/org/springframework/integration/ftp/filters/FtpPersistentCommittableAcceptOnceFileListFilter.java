/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.ftp.filters;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.AbstractPersistentCommittableAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * Persistent file list filter using the FileProcessingRecord to see if we already
 * 'seen' this file.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public class FtpPersistentCommittableAcceptOnceFileListFilter extends AbstractPersistentCommittableAcceptOnceFileListFilter<FTPFile> {

	public FtpPersistentCommittableAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(FTPFile file) {
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	protected String fileName(FTPFile file) {
		return file.getName();
	}
}
