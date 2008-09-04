/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Composition that delegates to multiple {@link FileFilter}s. The compostition
 * is AND based, meaning that all filters must {@link #accept(File)} in order
 * for a file to be accepted by the composite.
 * 
 * @author Iwein Fuld
 */
class CompositeFileFilter implements FileFilter {

	private final Set<FileFilter> fileFilters;

	public CompositeFileFilter(FileFilter... fileFilters) {
		this.fileFilters = new HashSet<FileFilter>(Arrays.asList(fileFilters));
	}

	public CompositeFileFilter(HashSet<FileFilter> fileFilters) {
		this.fileFilters = new HashSet<FileFilter>(fileFilters);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean accept(File pathname) {
		for (FileFilter fileFilter : fileFilters) {
			if (!fileFilter.accept(pathname)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see #addFilters(Collection)
	 * @param filters one or more new filters to be used
	 * @return a new CompositeFileFilter with the additional filters
	 */
	public CompositeFileFilter addFilter(FileFilter... filters) {
		return addFilters(Arrays.asList(filters));
	}

	/**
	 * Creates a new CompositeFileFilter that delegates to both the filters of
	 * this Composite and the new filters passed in.
	 * 
	 * @param filtersToAdd
	 * @return a new CompositeFileFilter with the added filters
	 */
	public CompositeFileFilter addFilters(Collection<FileFilter> filtersToAdd) {
		HashSet<FileFilter> newFilterSet = new HashSet<FileFilter>(filtersToAdd);
		newFilterSet.addAll(fileFilters);
		return new CompositeFileFilter(newFilterSet);
	}
}
