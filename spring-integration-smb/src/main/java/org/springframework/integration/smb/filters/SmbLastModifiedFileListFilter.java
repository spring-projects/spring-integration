/*
 * Copyright 2023 the original author or authors.
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
import java.util.function.Consumer;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractLastModifiedFileListFilter;

/**
 * The {@link org.springframework.integration.file.filters.FileListFilter} implementation to filter those files which
 * {@link SmbFile#getLastModified()} is less than the age in comparison
 * with the current time.
 * <p>
 *     The resolution is done in seconds.
 * </p>
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 *
 * @since 6.2
 */
public class SmbLastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<SmbFile> {

	public SmbLastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link SmbLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SmbLastModifiedFileListFilter(long age) {
		super(Duration.ofSeconds(age));
	}

	@Override
	protected Instant getLastModified(SmbFile remoteFile) {
		return Instant.ofEpochSecond(remoteFile.getLastModified() / ONE_SECOND);
	}

}
