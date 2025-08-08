/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;

/**
 * {@link FileListFilter} implementation that ignores any hidden files. Uses
 * {@link File#isHidden()} to make that determination.
 *
 * @author Gunnar Hillert
 * @since 4.2
 */
public class IgnoreHiddenFileListFilter extends AbstractFileListFilter<File> {

	/**
	 * @return Returns {@code true} for any non-hidden files.
	 */
	@Override
	public boolean accept(File file) {
		return !file.isHidden();
	}

}
