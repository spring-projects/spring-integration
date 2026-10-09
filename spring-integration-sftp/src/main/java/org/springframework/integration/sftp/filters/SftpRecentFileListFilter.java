/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import java.time.Duration;
import java.time.Instant;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractRecentFileListFilter;

/**
 * The {@link AbstractRecentFileListFilter} implementation for SFTP protocol.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class SftpRecentFileListFilter extends AbstractRecentFileListFilter<SftpClient.DirEntry> {

	public SftpRecentFileListFilter() {
		super();
	}

	public SftpRecentFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(SftpClient.DirEntry remoteFile) {
		return remoteFile.getAttributes().getModifyTime().toInstant();
	}

}
