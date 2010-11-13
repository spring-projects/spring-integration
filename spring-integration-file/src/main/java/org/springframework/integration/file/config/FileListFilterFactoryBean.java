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
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.file.filters.*;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter<File>> {

	private volatile FileListFilter<File> result;

	private volatile FileListFilter<File> filter;

	private volatile String filenamePattern;

	private volatile String filenameRegex;

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
		FileListFilter<File> filter;
		if ((this.filter != null) && (this.filenamePattern != null || this.filenameRegex!=null)) {
			throw new IllegalArgumentException("The 'filter' reference is mutually exclusive with "
					+ "'filename-pattern' and 'filename-regex' attributes.");
		}

		//'filter' is set
		if (this.filter != null) {
			if (Boolean.TRUE.equals(this.preventDuplicates)) {
				filter = this.createCompositeWithAcceptOnceFilter(this.filter);
			}
			else { // preventDuplicates is either FALSE or NULL
				filter = this.filter;
			}
		}

		// 'file-pattern' or 'file-regex' is set
		else if (this.filenamePattern != null || this.filenameRegex!=null) {
			List<FileListFilter<File>> filtersNeeded = new ArrayList<FileListFilter<File>>();
			if (!Boolean.FALSE.equals(this.preventDuplicates)) {
				//preventDuplicates is either null or true
				filtersNeeded.add(new AcceptOnceFileListFilter<File>());
			}
			if (this.filenamePattern!=null){
				filtersNeeded.add(new SimplePatternFileListFilter(this.filenamePattern));
			}
			if (this.filenameRegex!=null){
				filtersNeeded.add(new PatternMatchingFileListFilter(this.filenameRegex));
			}
			if (filtersNeeded.size()==1){
				filter = filtersNeeded.get(0);
			}
			else {
				filter = new CompositeFileListFilter<File>(filtersNeeded);
			}
		}

		// no filters are provided
		else if (Boolean.FALSE.equals(this.preventDuplicates)) {
			filter = new AcceptAllFileListFilter<File>();
		}
		else { // preventDuplicates is either TRUE or NULL
			filter = new AcceptOnceFileListFilter<File>();
		}

		if (filter == null) {
			filter = new CompositeFileListFilter<File>();
		}
		this.result = filter;
	}

	private CompositeFileListFilter<File> createCompositeWithAcceptOnceFilter(FileListFilter<File> otherFilter) {
		CompositeFileListFilter<File> compositeFilter = new CompositeFileListFilter<File>();
		compositeFilter.addFilter(new AcceptOnceFileListFilter<File>());
		compositeFilter.addFilter(otherFilter);
		return compositeFilter;
	}

}
