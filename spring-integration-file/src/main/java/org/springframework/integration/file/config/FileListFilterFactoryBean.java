/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.lang.NonNull;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter<File>> {

	private volatile FileListFilter<File> result;

	private volatile FileListFilter<File> filter;

	private volatile String filenamePattern;

	private volatile String filenameRegex;

	private volatile Boolean ignoreHidden = Boolean.TRUE;

	private volatile Boolean preventDuplicates;

	private volatile Boolean alwaysAcceptDirectories;

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
	@NonNull
	public FileListFilter<File> getObject() {
		if (this.result == null) {
			this.monitor.lock();
			try {
				this.initializeFileListFilter();
			}
			finally {
				this.monitor.unlock();
			}
		}
		return this.result;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.result != null) ? this.result.getClass() : FileListFilter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private void initializeFileListFilter() {
		if (this.result != null) {
			return;
		}
		FileListFilter<File> createdFilter = null;

		validate();

		final List<FileListFilter<File>> filtersNeeded = new ArrayList<FileListFilter<File>>();

		if (!Boolean.FALSE.equals(this.ignoreHidden)) {
			filtersNeeded.add(new IgnoreHiddenFileListFilter());
		}

		//'filter' is set
		if (this.filter != null) {
			filter(filtersNeeded);
		}

		// 'file-pattern' or 'file-regex' is set
		else if (this.filenamePattern != null || this.filenameRegex != null) {
			pattern(filtersNeeded);
		}

		// no filters are provided
		else if (Boolean.FALSE.equals(this.preventDuplicates)) {
			filtersNeeded.add(new AcceptAllFileListFilter<File>());
		}
		else { // preventDuplicates is either TRUE or NULL
			filtersNeeded.add(new AcceptOnceFileListFilter<File>());
		}

		if (filtersNeeded.size() == 1) {
			createdFilter = filtersNeeded.get(0);
		}
		else {
			createdFilter = new CompositeFileListFilter<File>(filtersNeeded);
		}

		this.result = createdFilter;
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

	private void filter(final List<FileListFilter<File>> filtersNeeded) {
		if (Boolean.TRUE.equals(this.preventDuplicates)) {
			filtersNeeded.add(new AcceptOnceFileListFilter<File>());
			filtersNeeded.add(this.filter);
		}
		else { // preventDuplicates is either FALSE or NULL
			filtersNeeded.add(this.filter);
		}
	}

	private void pattern(final List<FileListFilter<File>> filtersNeeded) {
		if (!Boolean.FALSE.equals(this.preventDuplicates)) {
			//preventDuplicates is either null or true
			filtersNeeded.add(new AcceptOnceFileListFilter<File>());
		}
		if (this.filenamePattern != null) {
			SimplePatternFileListFilter patternFilter = new SimplePatternFileListFilter(this.filenamePattern);
			if (this.alwaysAcceptDirectories != null) {
				patternFilter.setAlwaysAcceptDirectories(this.alwaysAcceptDirectories);
			}
			filtersNeeded.add(patternFilter);
		}
		if (this.filenameRegex != null) {
			RegexPatternFileListFilter regexFilter = new RegexPatternFileListFilter(this.filenameRegex);
			if (this.alwaysAcceptDirectories != null) {
				regexFilter.setAlwaysAcceptDirectories(this.alwaysAcceptDirectories);
			}
			filtersNeeded.add(regexFilter);
		}
	}

}
