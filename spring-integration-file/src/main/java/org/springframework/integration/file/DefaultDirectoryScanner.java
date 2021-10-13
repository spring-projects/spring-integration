/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.messaging.MessagingException;

/**
 * Default directory scanner and base class for other directory scanners.
 * Manages the default interrelations between filtering, scanning and locking.
 *
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class DefaultDirectoryScanner implements DirectoryScanner {

	private volatile FileListFilter<File> filter;

	private volatile FileLocker locker;

	/**
	 * Initialize {@link DefaultDirectoryScanner#filter} with a default list of
	 * {@link FileListFilter}s using a {@link CompositeFileListFilter}:
	 * <ul>
	 *	<li>{@link IgnoreHiddenFileListFilter}</li>
	 *	<li>{@link AcceptOnceFileListFilter}</li>
	 * </ul>.
	 */
	public DefaultDirectoryScanner() {
		final List<FileListFilter<File>> defaultFilters = new ArrayList<>(2);
		defaultFilters.add(new IgnoreHiddenFileListFilter());
		defaultFilters.add(new AcceptOnceFileListFilter<>());
		this.filter = new CompositeFileListFilter<>(defaultFilters);
	}

	@Override
	public void setFilter(FileListFilter<File> filter) {
		this.filter = filter;
	}

	protected FileListFilter<File> getFilter() {
		return this.filter;
	}

	@Override
	public final void setLocker(FileLocker locker) {
		this.locker = locker;
	}

	protected FileLocker getLocker() {
		return this.locker;
	}

	/**
	 * This class takes the minimal implementation and merely delegates to the locker if set.
	 * @param file the file to try to claim.
	 */
	@Override
	public boolean tryClaim(File file) {
		return (this.locker == null) || this.locker.lock(file);
	}

	@Override
	public List<File> listFiles(File directory) throws IllegalArgumentException {
		File[] files = listEligibleFiles(directory);
		if (files == null) {
			throw new MessagingException("The path [" + directory
					+ "] does not denote a properly accessible directory.");
		}
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	/**
	 * Subclasses may refine the listing strategy by overriding this method. The
	 * files returned here are passed onto the filter.
	 * @param directory root directory to use for listing
	 * @return the files this scanner should consider
	 */
	protected File[] listEligibleFiles(File directory) {
		return directory.listFiles();
	}

}
