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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Composition that delegates to multiple {@link FileFilter}s. The composition
 * is AND based, meaning that all filters must {@link #accept(File)} in order
 * for a File to be accepted by the composite.
 * 
 * @author Iwein Fuld
 */
public class CompositeFileFilter implements FileFilter {

	private final List<FileFilter> fileFilters;


	public CompositeFileFilter(FileFilter... fileFilters) {
		this.fileFilters = new ArrayList<FileFilter>(Arrays.asList(fileFilters));
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

	public void addFilter(FileFilter... filters) {
		fileFilters.addAll(Arrays.asList(filters));
	}

	public static CompositeFileFilter with(FileFilter... fileFilters) {
		return new CompositeFileFilter(fileFilters);
	}

}
