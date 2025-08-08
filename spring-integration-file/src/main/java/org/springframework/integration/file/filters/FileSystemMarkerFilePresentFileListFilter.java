/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

/**
 * File system implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class FileSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<File> {

	public FileSystemMarkerFilePresentFileListFilter(FileListFilter<File> filter) {
		super(filter);
	}

	public FileSystemMarkerFilePresentFileListFilter(FileListFilter<File> filter, String suffix) {
		super(filter, suffix);
	}

	public FileSystemMarkerFilePresentFileListFilter(FileListFilter<File> filter,
			Function<String, String> function) {
		super(filter, function);
	}

	public FileSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<File>, Function<String, String>> filtersAndFunctions) {
		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(File file) {
		return file.getName();
	}

}
