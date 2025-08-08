/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractLastModifiedFileListFilter;

/**
 * The {@link AbstractLastModifiedFileListFilter} implementation to filter those files which
 * {@link FileTime#toInstant()} is less than the age in comparison with the {@link Instant#now()}.
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class SftpLastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<SftpClient.DirEntry> {

	public SftpLastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link SftpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SftpLastModifiedFileListFilter(long age) {
		this(Duration.ofSeconds(age));
	}

	/**
	 * Construct a {@link SftpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the Duration
	 */
	public SftpLastModifiedFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(SftpClient.DirEntry remoteFile) {
		return remoteFile.getAttributes().getModifyTime().toInstant();
	}

}
