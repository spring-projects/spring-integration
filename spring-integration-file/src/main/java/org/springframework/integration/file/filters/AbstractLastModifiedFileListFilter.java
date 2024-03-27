/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;

/**
 * The {@link FileListFilter} implementation to filter those files which
 * lastModified is less than the {@link #age} in comparison
 * with the {@link Instant#now()}.
 * When {@link #discardCallback} is provided, it called for all the rejected files.
 *
 * @param <F> the file
 *
 * @author Adama Sorho
 * @author Artem Bilan
 *
 * @since 6.2
 */
public abstract class AbstractLastModifiedFileListFilter<F> implements DiscardAwareFileListFilter<F> {

	protected static final long ONE_SECOND = 1000;

	private static final long DEFAULT_AGE = 60;

	private Duration age = Duration.ofSeconds(DEFAULT_AGE);

	@Nullable
	private Consumer<F> discardCallback;

	public AbstractLastModifiedFileListFilter() {
	}

	public AbstractLastModifiedFileListFilter(Duration age) {
		this.age = age;
	}

	/**
	 * Set the age that files have to be before being passed by this filter.
	 * If lastModified plus {@link #age} is before the {@link Instant#now()}, the file
	 * is filtered.
	 * Defaults to 60 seconds.
	 * @param age the Duration.
	 */
	public void setAge(Duration age) {
		this.age = age;
	}

	/**
	 * Set the age that files have to be before being passed by this filter.
	 * If lastModified plus {@link #age} is before the {@link Instant#now()}, the file
	 * is filtered.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public void setAge(long age) {
		setAge(Duration.ofSeconds(age));
	}

	@Override
	public void addDiscardCallback(@Nullable Consumer<F> discardCallback) {
		this.discardCallback = discardCallback;
	}

	@Override
	public List<F> filterFiles(F[] files) {
		List<F> list = new ArrayList<>();
		Instant now = Instant.now();
		for (F file : files) {
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
	public boolean accept(F file) {
		if (fileIsAged(file, Instant.now())) {
			return true;
		}
		else if (this.discardCallback != null) {
			this.discardCallback.accept(file);
		}

		return false;
	}

	private boolean fileIsAged(F file, Instant now) {
		return getLastModified(file).plus(this.age).isBefore(now);
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

	protected Duration getAgeDuration() {
		return this.age;
	}

	protected abstract Instant getLastModified(F remoteFile);

}
