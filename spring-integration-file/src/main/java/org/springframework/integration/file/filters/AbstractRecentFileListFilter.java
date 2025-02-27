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

package org.springframework.integration.file.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link FileListFilter} to accept only files which are recent according to provided {@code age}:
 * the {@code lastModified} of the file is more than the age in comparison with the current time.
 * In other words, accept those files which are not old enough yet.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public abstract class AbstractRecentFileListFilter<F> implements FileListFilter<F> {

	protected static final long ONE_SECOND = 1000;

	private final Duration age;

	/**
	 * Construct an instance with default age as 1 day.
	 */
	public AbstractRecentFileListFilter() {
		this(Duration.ofDays(1));
	}

	public AbstractRecentFileListFilter(Duration age) {
		this.age = age;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

	@Override
	public List<F> filterFiles(F[] files) {
		List<F> list = new ArrayList<>();
		Instant now = Instant.now();
		for (F file : files) {
			if (!fileIsAged(file, now)) {
				list.add(file);
			}
		}

		return list;
	}

	@Override
	public boolean accept(F file) {
		return !fileIsAged(file, Instant.now());
	}

	protected boolean fileIsAged(F file, Instant now) {
		return getLastModified(file).plus(this.age).isBefore(now);
	}

	protected abstract Instant getLastModified(F remoteFile);

}
