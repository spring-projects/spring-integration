/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Implementation of AbstractRegexPatternMatchingFileListFilter for java.io.File instances.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<File> {

	public RegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public RegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(File file) {
		return (file != null) ? file.getName() : null;
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
