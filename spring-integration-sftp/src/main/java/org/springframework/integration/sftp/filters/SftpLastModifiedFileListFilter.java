/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.DiscardAwareFileListFilter;
import org.springframework.lang.Nullable;

/**
 * The {@link org.springframework.integration.file.filters.FileListFilter} implementation to filter those files which
 * {@link FileTime#toInstant()} is less than the {@link #age} in comparison
 * with the {@link Instant#now()}.
 * When {@link #discardCallback} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 *
 * @since 6.2
 */
public class SftpLastModifiedFileListFilter implements DiscardAwareFileListFilter<SftpClient.DirEntry> {

	private static final long DEFAULT_AGE = 60;

	private Duration age = Duration.ofSeconds(DEFAULT_AGE);

	@Nullable
	private Consumer<SftpClient.DirEntry> discardCallback;

	public SftpLastModifiedFileListFilter() {
	}

	/**
	 * Construct a {@link SftpLastModifiedFileListFilter} instance with provided {@link #age}.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SftpLastModifiedFileListFilter(long age) {
		this(Duration.ofSeconds(age));
	}

	public SftpLastModifiedFileListFilter(Duration age) {
		this.age = age;
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link FileTime#toInstant()} plus {@link #age} is before the {@link Instant#now()}, the file
	 * is filtered.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 * @param unit the timeUnit.
	 */
	public void setAge(long age, TimeUnit unit) {
		setAge(Duration.ofSeconds(unit.toSeconds(age)));
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link FileTime#toInstant()} plus {@link #age} is before the {@link Instant#now()}, the file
	 * is filtered.
	 * Defaults to 60 seconds.
	 * @param age The Duration.
	 */
	public void setAge(Duration age) {
		this.age = age;
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link FileTime#toInstant()} plus {@link #age} is before the {@link Instant#now()}, the file
	 * is filtered.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public void setAge(long age) {
		setAge(Duration.ofSeconds(age));
	}

	@Override
	public void addDiscardCallback(@Nullable Consumer<SftpClient.DirEntry> discardCallback) {
		this.discardCallback = discardCallback;
	}

	@Override
	public List<SftpClient.DirEntry> filterFiles(SftpClient.DirEntry[] files) {
		List<SftpClient.DirEntry> list = new ArrayList<>();
		Instant now = Instant.now();
		for (SftpClient.DirEntry file: files) {
			if (fileIsAged(file, now)) {
				list.add(file);
			}
			else if (this.discardCallback != null) {
				this.discardCallback.accept(file);
			}
		}

		return list;
	}

	@Override
	public boolean accept(SftpClient.DirEntry file) {
		if (fileIsAged(file, Instant.now())) {
			return true;
		}
		else if (this.discardCallback != null) {
			this.discardCallback.accept(file);
		}

		return false;
	}

	private boolean fileIsAged(SftpClient.DirEntry file, Instant now) {
		return file.getAttributes().getModifyTime().toInstant().plus(this.age).isBefore(now);
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

}
