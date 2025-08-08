/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * Implementation of {@link AbstractSimplePatternFileListFilter} for FTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class FtpSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<FTPFile> {

	public FtpSimplePatternFileListFilter(String pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(FTPFile file) {
		return (file != null) ? file.getName() : null;
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file.isDirectory();
	}

}
