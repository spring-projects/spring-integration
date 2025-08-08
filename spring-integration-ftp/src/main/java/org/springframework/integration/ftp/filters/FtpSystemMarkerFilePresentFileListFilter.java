/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * FTP implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class FtpSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<FTPFile> {

	public FtpSystemMarkerFilePresentFileListFilter(FileListFilter<FTPFile> filter) {
		super(filter);
	}

	public FtpSystemMarkerFilePresentFileListFilter(FileListFilter<FTPFile> filter, String suffix) {
		super(filter, suffix);
	}

	public FtpSystemMarkerFilePresentFileListFilter(FileListFilter<FTPFile> filter,
			Function<String, String> function) {
		super(filter, function);
	}

	public FtpSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<FTPFile>, Function<String, String>> filtersAndFunctions) {
		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(FTPFile file) {
		return file.getName();
	}

}
