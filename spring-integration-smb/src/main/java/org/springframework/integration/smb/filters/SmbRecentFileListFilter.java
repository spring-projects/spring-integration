/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.integration.smb.filters;

import java.time.Duration;
import java.time.Instant;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractRecentFileListFilter;

/**
 * The {@link AbstractRecentFileListFilter} implementation for SMB protocol.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class SmbRecentFileListFilter extends AbstractRecentFileListFilter<SmbFile> {

	public SmbRecentFileListFilter() {
		super();
	}

	public SmbRecentFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(SmbFile remoteFile) {
		return Instant.ofEpochSecond(remoteFile.getLastModified() / ONE_SECOND);
	}

}
