/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * Implementation of {@link AbstractSimplePatternFileListFilter} for SFTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<SftpClient.DirEntry> {

	public SftpSimplePatternFileListFilter(String pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(SftpClient.DirEntry entry) {
		return (entry != null) ? entry.getFilename() : null;
	}

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file.getAttributes().isDirectory();
	}

}
