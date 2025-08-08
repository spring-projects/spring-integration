/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Filters a listing of files by qualifying their 'name'
 * against a regular expression (an instance of {@link Pattern}).
 *
 * @param <F> the type of file entry
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractRegexPatternFileListFilter<F> extends AbstractDirectoryAwareFileListFilter<F> {

	private Pattern pattern;

	public AbstractRegexPatternFileListFilter(String pattern) {
		this(Pattern.compile(pattern));
	}

	public AbstractRegexPatternFileListFilter(Pattern pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		this.pattern = pattern;
	}

	public void setPattern(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		setPattern(Pattern.compile(pattern));
	}

	public void setPattern(Pattern pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		this.pattern = pattern;
	}

	@Override
	public boolean accept(F file) {
		return alwaysAccept(file) || (file != null && this.pattern.matcher(getFilename(file)).matches());
	}

	/**
	 * Subclasses must implement this method to extract the file's name.
	 * @param file The file.
	 * @return The file name.
	 */
	protected abstract String getFilename(F file);

}
