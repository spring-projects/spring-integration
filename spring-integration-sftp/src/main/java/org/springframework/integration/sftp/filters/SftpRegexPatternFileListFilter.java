/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import java.util.regex.Pattern;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SFTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<SftpClient.DirEntry> {

	public SftpRegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public SftpRegexPatternFileListFilter(Pattern pattern) {
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
