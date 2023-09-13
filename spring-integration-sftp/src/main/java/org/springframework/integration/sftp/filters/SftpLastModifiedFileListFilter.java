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

package org.springframework.integration.sftp.filters;

import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractLastModifiedFileListFilter;

/**
 * The {@link org.springframework.integration.file.filters.FileListFilter} implementation to filter those files which
 * {@link FileTime#toInstant()} is less than the age in comparison
 * with the {@link Instant#now()}.
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 *
 * @since 6.2
 */
public class SftpLastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<SftpClient.DirEntry> {

	public SftpLastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link SftpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SftpLastModifiedFileListFilter(long age) {
		this(Duration.ofSeconds(age));
	}

	/**
	 * Construct a {@link SftpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the Duration
	 */
	public SftpLastModifiedFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(SftpClient.DirEntry remoteFile) {
		return remoteFile.getAttributes().getModifyTime().toInstant();
	}

}
