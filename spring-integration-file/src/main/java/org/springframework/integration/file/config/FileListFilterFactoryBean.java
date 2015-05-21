/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter<File>> {

	private volatile FileListFilter<File> result;

	private volatile FileListFilter<File> filter;

	private volatile String filenamePattern;

	private volatile String filenameRegex;

	private volatile Boolean ignoreHidden = Boolean.TRUE;

	private volatile Boolean preventDuplicates;

	private final Object monitor = new Object();

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

	public FileListFilter<File> getObject() throws Exception {
		if (this.result == null) {
			synchronized (this.monitor) {
				this.initializeFileListFilter();
			}
		}
		return this.result;
	}

	public Class<?> getObjectType() {
		return (this.result != null) ? this.result.getClass() : FileListFilter.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeFileListFilter() {
		if (this.result != null) {
			return;
		}
		FileListFilter<File> createdFilter = null;
		if ((this.filter != null) && (this.filenamePattern != null || this.filenameRegex != null)) {
			throw new IllegalArgumentException("The 'filter' reference is mutually exclusive with "
					+ "either the 'filename-pattern' or 'filename-regex' attribute.");
		}

		if (this.filenamePattern != null && this.filenameRegex != null) {
			throw new IllegalArgumentException("The 'filename-pattern' and 'filename-regex' attributes are mutually exclusive.");
		}

		final List<FileListFilter<File>> filtersNeeded = new ArrayList<FileListFilter<File>>();

		if (!Boolean.FALSE.equals(this.ignoreHidden)) {
			filtersNeeded.add(new IgnoreHiddenFileListFilter());
		}

		//'filter' is set
		if (this.filter != null) {
			if (Boolean.TRUE.equals(this.preventDuplicates)) {
				filtersNeeded.add(new AcceptOnceFileListFilter<File>());
				filtersNeeded.add(this.filter);
			}
			else { // preventDuplicates is either FALSE or NULL
				filtersNeeded.add(this.filter);
			}
		}

		// 'file-pattern' or 'file-regex' is set
		else if (this.filenamePattern != null || this.filenameRegex != null) {

			if (!Boolean.FALSE.equals(this.preventDuplicates)) {
				//preventDuplicates is either null or true
				filtersNeeded.add(new AcceptOnceFileListFilter<File>());
			}
			if (this.filenamePattern != null) {
				filtersNeeded.add(new SimplePatternFileListFilter(this.filenamePattern));
			}
			if (this.filenameRegex != null) {
				filtersNeeded.add(new RegexPatternFileListFilter(this.filenameRegex));
			}
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

}
