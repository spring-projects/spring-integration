/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import java.util.Map;
import java.util.function.Function;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * SFTP implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class SftpSystemMarkerFilePresentFileListFilter
		extends AbstractMarkerFilePresentFileListFilter<SftpClient.DirEntry> {

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter) {
		super(filter);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter, String suffix) {
		super(filter, suffix);
	}

	public SftpSystemMarkerFilePresentFileListFilter(FileListFilter<SftpClient.DirEntry> filter,
			Function<String, String> function) {

		super(filter, function);
	}

	public SftpSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<SftpClient.DirEntry>, Function<String, String>> filtersAndFunctions) {

		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(SftpClient.DirEntry file) {
		return file.getFilename();
	}

}
