/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.util.List;

/**
 * Strategy interface for filtering a group of files. This is a generic filter intended
 * to work with either local files or references to remote files.
 *
 * @author Iwein Fuld
 * @author Josh Long
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface FileListFilter<F> {

	/**
	 * Filters out files and returns the files that are left in a list, or an
	 * empty list when a null is passed in.
	 * @param files The files.
	 * @return The filtered files.
	 */
	List<F> filterFiles(F[] files);

	/**
	 * Filter a single file.
	 * @param file the file.
	 * @return true if the file passes the filter, false to filter.
	 * @since 5.2
	 */
	default boolean accept(F file) {
		throw new UnsupportedOperationException(
				"Filters that return true in supportsSingleFileFiltering() must implement this method");
	}

	/**
	 * Indicates that this filter supports filtering a single file.
	 * Default false.
	 * @return true to allow external calls to {@link #accept(Object)}.
	 * @since 5.2
	 */
	default boolean supportsSingleFileFiltering() {
		return false;
	}

}
