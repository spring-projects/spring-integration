/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.ftp.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractLastModifiedFileListFilter;

/**
 * The {@link AbstractLastModifiedFileListFilter} implementation to filter those files which
 * {@link FTPFile#getTimestampInstant()} is less than the age in comparison with the {@link Instant#now()}.
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class FtpLastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<FTPFile> {

	public FtpLastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link FtpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public FtpLastModifiedFileListFilter(long age) {
		this(Duration.ofSeconds(age));
	}

	/**
	 * Construct a {@link FtpLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the Duration
	 */
	public FtpLastModifiedFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(FTPFile remoteFile) {
		return remoteFile.getTimestampInstant();
	}

}
