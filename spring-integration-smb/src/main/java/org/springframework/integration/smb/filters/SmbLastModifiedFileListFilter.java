/*
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.codelibs.jcifs.smb.impl.SmbFile;

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
 * @author Daniel Frey
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
