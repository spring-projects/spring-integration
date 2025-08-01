/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter<File>> {

	private volatile @Nullable FileListFilter<File> result;

	private volatile @Nullable FileListFilter<File> filter;

	private volatile @Nullable String filenamePattern;

	private volatile @Nullable String filenameRegex;

	private volatile Boolean ignoreHidden = Boolean.TRUE;

	private volatile @Nullable Boolean preventDuplicates;

	private volatile @Nullable Boolean alwaysAcceptDirectories;

	private final Lock monitor = new ReentrantLock();

	public void setFilter(FileListFilter<File> filter) {
		this.filter = filter;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public void setFilenameRegex(String filenameRegex) {
		this.filenameRegex = filenameRegex;
	}

	/**
	 * Specify whether hidden files shall be ignored.
	 * This is {@code true} by default.
	 * @param ignoreHidden Can be null, which triggers default behavior.
	 * @since 4.2
	 */
	public void setIgnoreHidden(Boolean ignoreHidden) {
		this.ignoreHidden = ignoreHidden;
	}

	public void setPreventDuplicates(Boolean preventDuplicates) {
		this.preventDuplicates = preventDuplicates;
	}

	/**
	 * Set to true to indicate that the pattern should not be applied to directories.
	 * Used for recursive scans for file patterns, for example in gateway recursive
	 * mget operations. Only applies when a pattern or regex is provided.
	 * @param alwaysAcceptDirectories true to always pass directories.
	 * @since 5.0
	 */
	public void setAlwaysAcceptDirectories(Boolean alwaysAcceptDirectories) {
		this.alwaysAcceptDirectories = alwaysAcceptDirectories;
	}

	@Override
	public FileListFilter<File> getObject() {
		FileListFilter<File> filterToReturn = this.result;
		if (filterToReturn == null) {
			this.monitor.lock();
			try {
				filterToReturn = this.result;
				if (filterToReturn == null) {
					filterToReturn = initializeFileListFilter();
					this.result = filterToReturn;
				}
			}
			finally {
				this.monitor.unlock();
			}
		}
		return filterToReturn;
	}

	@Override
	public Class<?> getObjectType() {
		FileListFilter<File> filterToCheck = this.result;
		return (filterToCheck != null) ? filterToCheck.getClass() : FileListFilter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private FileListFilter<File> initializeFileListFilter() {
		validate();

		final List<FileListFilter<File>> filtersNeeded = new ArrayList<>();

		if (!Boolean.FALSE.equals(this.ignoreHidden)) {
			filtersNeeded.add(new IgnoreHiddenFileListFilter());
		}

		//'filter' is set
		FileListFilter<File> filterToAdd = this.filter;
		if (filterToAdd != null) {
			filter(filtersNeeded, filterToAdd);
		}

		// 'file-pattern' or 'file-regex' is set
		else if (this.filenamePattern != null || this.filenameRegex != null) {
			pattern(filtersNeeded);
		}

		// no filters are provided
		else if (Boolean.FALSE.equals(this.preventDuplicates)) {
			filtersNeeded.add(new AcceptAllFileListFilter<>());
		}
		else { // preventDuplicates is either TRUE or NULL
			filtersNeeded.add(new AcceptOnceFileListFilter<>());
		}

		if (filtersNeeded.size() == 1) {
			return filtersNeeded.get(0);
		}
		else {
			return new CompositeFileListFilter<>(filtersNeeded);
		}
	}

	private void validate() {
		if ((this.filter != null) && (this.filenamePattern != null || this.filenameRegex != null)) {
			throw new IllegalArgumentException("The 'filter' reference is mutually exclusive with "
					+ "either the 'filename-pattern' or 'filename-regex' attribute.");
		}

		if (this.filenamePattern != null && this.filenameRegex != null) {
			throw new IllegalArgumentException("The 'filename-pattern' and 'filename-regex' attributes are "
					+ "mutually exclusive.");
		}
	}

	private void filter(final List<FileListFilter<File>> filtersNeeded, FileListFilter<File> filter) {
		if (Boolean.TRUE.equals(this.preventDuplicates)) {
			filtersNeeded.add(new AcceptOnceFileListFilter<>());
			filtersNeeded.add(filter);
		}
		else { // preventDuplicates is either FALSE or NULL
			filtersNeeded.add(filter);
		}
	}

	private void pattern(final List<FileListFilter<File>> filtersNeeded) {
		if (!Boolean.FALSE.equals(this.preventDuplicates)) {
			//preventDuplicates is either null or true
			filtersNeeded.add(new AcceptOnceFileListFilter<>());
		}
		String filenamePatternToUse = this.filenamePattern;
		Boolean alwaysAcceptDirectoriesToUse = this.alwaysAcceptDirectories;
		if (filenamePatternToUse != null) {
			SimplePatternFileListFilter patternFilter = new SimplePatternFileListFilter(filenamePatternToUse);
			if (alwaysAcceptDirectoriesToUse != null) {
				patternFilter.setAlwaysAcceptDirectories(alwaysAcceptDirectoriesToUse);
			}
			filtersNeeded.add(patternFilter);
		}
		String filenameRegexToUse = this.filenameRegex;
		if (filenameRegexToUse != null) {
			RegexPatternFileListFilter regexFilter = new RegexPatternFileListFilter(filenameRegexToUse);
			if (alwaysAcceptDirectoriesToUse != null) {
				regexFilter.setAlwaysAcceptDirectories(alwaysAcceptDirectoriesToUse);
			}
			filtersNeeded.add(regexFilter);
		}
	}

}
