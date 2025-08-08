/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractLastModifiedFileListFilter;

/**
 * The {@link AbstractLastModifiedFileListFilter} implementation to filter those files which
 * {@link SmbFile#getLastModified()} is less than the age in comparison with the current time.
 * <p>
 *     The resolution is done in seconds.
 * </p>
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the rejected files.
 *
 * @author Adama Sorho
 *
 * @since 6.2
 */
public class SmbLastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<SmbFile> {

	public SmbLastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link SmbLastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 */
	public SmbLastModifiedFileListFilter(long age) {
		super(Duration.ofSeconds(age));
	}

	@Override
	protected Instant getLastModified(SmbFile remoteFile) {
		return Instant.ofEpochSecond(remoteFile.getLastModified() / ONE_SECOND);
	}

}
