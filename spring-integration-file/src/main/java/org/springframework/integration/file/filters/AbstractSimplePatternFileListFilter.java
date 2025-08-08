/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import org.springframework.util.AntPathMatcher;

/**
 * Base class for filters that support ant style path expressions, which are less powerful
 * but more readable than regular expressions. This filter only filters on the name of the
 * file, the rest of the path is ignored.
 *
 * @param <F> the file type.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 *
 * @since 2.0
 *
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter
 */
public abstract class AbstractSimplePatternFileListFilter<F> extends AbstractDirectoryAwareFileListFilter<F> {

	private final AntPathMatcher matcher = new AntPathMatcher();

	private final String path;

	public AbstractSimplePatternFileListFilter(String path) {
		this.path = path;
	}

	/**
	 * Accept the given file if its name matches the pattern.
	 */
	@Override
	public final boolean accept(F file) {
		return alwaysAccept(file) || (file != null && this.matcher.match(this.path, this.getFilename(file)));
	}

	/**
	 * Subclasses must implement this method to extract the file's name.
	 *
	 * @param file The file.
	 * @return The file name.
	 */
	protected abstract String getFilename(F file);

}
