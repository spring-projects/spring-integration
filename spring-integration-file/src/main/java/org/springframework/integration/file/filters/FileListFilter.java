/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.util.List;

/**
 * Strategy interface for filtering a group of files. This is a generic filter intended
 * to work with either local files or references to remote files.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Gary Russell
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
	 * Filter a single file; only called externally if {@link #supportsSingleFileFiltering()}
	 * returns true.
	 * @param file the file.
	 * @return true if the file passes the filter, false to filter.
	 * @since 5.2
	 * @see #supportsSingleFileFiltering()
	 */
	default boolean accept(F file) {
		throw new UnsupportedOperationException(
				"Filters that return true in supportsSingleFileFiltering() must implement this method");
	}

	/**
	 * Indicates that this filter supports filtering a single file.
	 * Filters that return true <b>must</b> override {@link #accept(Object)}.
	 * Default false.
	 * @return true to allow external calls to {@link #accept(Object)}.
	 * @since 5.2
	 * @see #accept(Object)
	 */
	default boolean supportsSingleFileFiltering() {
		return false;
	}

	/**
	 * Return true if this filter is being used for recursion.
	 * @return whether or not to filter based on the full path.
	 * @since 5.3.6
	 */
	default boolean isForRecursion() {
		return false;
	}

}
