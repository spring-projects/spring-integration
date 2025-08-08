/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.filters;

/**
 * A {@link FileListFilter} that can be reset by removing a specific file from its
 * state.
 * @param <F> The type that will be filtered.
 *
 * @author Gary Russell
 *
 * @since 4.1.7
 */
public interface ResettableFileListFilter<F> extends FileListFilter<F> {

	/**
	 * Remove the specified file from the filter, so it will pass on the next attempt.
	 * @param f the element to remove.
	 * @return true if the file was removed as a result of this call.
	 */
	boolean remove(F f);

}
