/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import java.util.Map;
import java.util.function.Function;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * Implementation of {@link AbstractMarkerFilePresentFileListFilter} for SMB.
 *
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<SmbFile> {

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter) {
		super(filter);
	}

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter, String suffix) {
		super(filter, suffix);
	}

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter, Function<String, String> function) {
		super(filter, function);
	}

	public SmbSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<SmbFile>, Function<String, String>> filtersAndFunctions) {

		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(SmbFile file) {
		return file.getName();
	}

}
