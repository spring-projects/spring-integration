/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * Persistent file list filter using the server's file timestamp to detect if we've already
 * 'seen' this file.
 *
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
public class FtpPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<FTPFile> {

	public FtpPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(FTPFile file) {
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	protected String fileName(FTPFile file) {
		return file.getName();
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file.isDirectory();
	}

}
