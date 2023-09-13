/*
 * Copyright 2015-2023 the original author or authors.
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

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The {@link FileListFilter} implementation to filter those files which
 * {@link File#lastModified()} is less than the age in comparison
 * with the current time.
 * <p>
 * The resolution is done in seconds.
 * <p>
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the
 * rejected files.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Adama Sorho
 *
 * @since 4.2
 *
 */
public class LastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<File> {

	private static final long ONE_SECOND = 1000;

	public LastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link LastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 * @since 5.0
	 */
	public LastModifiedFileListFilter(long age) {
		super(Duration.ofSeconds(age));
	}

	/**
	 * Set the age that files have to be before being passed by this filter.
	 * If {@link File#lastModified()} plus age is greater than the current time, the file
	 * is filtered. The resolution is seconds.
	 * Defaults to 60 seconds.
	 * @param age the age
	 * @param unit the timeUnit.
	 */
	public void setAge(long age, TimeUnit unit) {
		setAge(unit.toSeconds(age));
	}

	public long getAge() {
		return getCurrentAge().getSeconds();
	}

	@Override
	protected Instant getLastModified(File file) {
		return Instant.ofEpochSecond(file.lastModified() / ONE_SECOND);
	}

}
