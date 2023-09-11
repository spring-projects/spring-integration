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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.DiscardAwareFileListFilter;
import org.springframework.lang.Nullable;

/**
 * The {@link org.springframework.integration.file.filters.FileListFilter} implementation to filter those files which
 * {@link SmbFile#getLastModified()} is less than the {@link #age} in comparison
 * with the current time.
 * <p>
 *     The resolution is done in seconds.
 * </p>
 * When {@link #discardCallback} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 *
 * @since 6.2
 */
public class SmbLastModifiedFileListFilter implements DiscardAwareFileListFilter<SmbFile> {

	private static final long ONE_SECOND = 1000;

	private static final long DEFAULT_AGE = 60;

	private long age = DEFAULT_AGE;

	@Nullable
	private Consumer<SmbFile> discardCallback;

	public SmbLastModifiedFileListFilter() {
	}

	/**
	 * Construct a {@link SmbLastModifiedFileListFilter} instance with provided {@link #age}.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SmbLastModifiedFileListFilter(long age) {
		this.age = age;
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link SmbFile#getLastModified()} plus age is greater than the current time, the file
	 * is filtered. The resolution is seconds.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 * @param unit the timeUnit.
	 */
	public void setAge(long age, TimeUnit unit) {
		this.age = unit.toSeconds(age);
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link SmbFile#getLastModified()} plus age is greater than the current time, the file
	 * is filtered. The resolution is seconds.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public void setAge(Duration age) {
		setAge(age.getSeconds());
	}

	/**
	 * Set the age that the files have to be before being passed by this filter.
	 * If {@link SmbFile#getLastModified()} plus age is greater than the current time, the file
	 * is filtered. The resolution is seconds.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public void setAge(long age) {
		setAge(age, TimeUnit.SECONDS);
	}

	@Override
	public void addDiscardCallback(@Nullable Consumer<SmbFile> discardCallback) {
		this.discardCallback = discardCallback;
	}

	@Override
	public List<SmbFile> filterFiles(SmbFile[] files) {
		List<SmbFile> list = new ArrayList<>();
		long now = System.currentTimeMillis() / ONE_SECOND;
		for (SmbFile file: files) {
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
	public boolean accept(SmbFile file) {
		if (fileIsAged(file, System.currentTimeMillis() / ONE_SECOND)) {
			return true;
		}
		else if (this.discardCallback != null) {
			this.discardCallback.accept(file);
		}

		return false;
	}

	private boolean fileIsAged(SmbFile file, long now) {
		return file.getLastModified() / ONE_SECOND + this.age <= now;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

}
