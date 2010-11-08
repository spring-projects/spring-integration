/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Collection;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter<File>> {

	private volatile FileListFilter<File> fileListFilter;

	private volatile FileListFilter<File> filterReference;

	private volatile String filenamePattern;

	private volatile Boolean preventDuplicates;

	private final Object monitor = new Object();

	private volatile Collection<FileListFilter<File>> filterReferences;


	public void setFilterReferences(Collection<FileListFilter<File>> filterReferences) {
		this.filterReferences = filterReferences;
	}

	public void setFilterReference(FileListFilter<File> filterReference) {
		this.filterReference = filterReference;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public void setPreventDuplicates(Boolean preventDuplicates) {
		this.preventDuplicates = preventDuplicates;
	}

	public FileListFilter<File> getObject() throws Exception {
		if (this.fileListFilter == null) {
			synchronized (this.monitor) {
				this.intializeFileListFilter();
			}
		}
		return this.fileListFilter;
	}

	public Class<?> getObjectType() {
		return (this.fileListFilter != null) ? this.fileListFilter.getClass() : FileListFilter.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void intializeFileListFilter() {
		if (this.fileListFilter != null) {
			return;
		}
		FileListFilter<File> filter;
		if ((this.filterReference != null) && (this.filenamePattern != null)) {
			throw new IllegalArgumentException("The 'filter' reference and "
					+ "'filename-pattern' attributes are mutually exclusive.");
		}

		if (this.filterReference != null) {
			if (Boolean.TRUE.equals(this.preventDuplicates)) {
				filter = this.createCompositeWithAcceptOnceFilter(this.filterReference);
			}
			else { // preventDuplicates is either FALSE or NULL
				filter = this.filterReference;
			}
		}
		else if (this.filenamePattern != null) {
			SimplePatternFileListFilter patternFilter = new SimplePatternFileListFilter(this.filenamePattern);
			if (Boolean.FALSE.equals(this.preventDuplicates)) {
				filter = patternFilter;
			}
			else { // preventDuplicates is either TRUE or NULL
				filter = this.createCompositeWithAcceptOnceFilter(patternFilter);
			}
		}
		else if (Boolean.FALSE.equals(this.preventDuplicates)) {
			filter = new AcceptAllFileListFilter<File>();
		}
		else { // preventDuplicates is either TRUE or NULL
			filter = new AcceptOnceFileListFilter<File>();
		}

		// finally, it might be that they simply want a {@link CompositeFileListFilter}
		if ((this.filterReferences != null) && (this.filterReferences.size() > 0)) {
			CompositeFileListFilter<File> compositeFilter = new CompositeFileListFilter<File>();
			for (FileListFilter<File> ff : filterReferences) {
				compositeFilter.addFilter(ff);
			}
			filter = compositeFilter;
		}
		if (filter == null) {
			filter = new CompositeFileListFilter<File>();
		}
		this.fileListFilter = filter;
	}

	private CompositeFileListFilter<File> createCompositeWithAcceptOnceFilter(FileListFilter<File> otherFilter) {
		CompositeFileListFilter<File> compositeFilter = new CompositeFileListFilter<File>();
		compositeFilter.addFilter(new AcceptOnceFileListFilter<File>());
		compositeFilter.addFilter(otherFilter);
		return compositeFilter;
	}

}
