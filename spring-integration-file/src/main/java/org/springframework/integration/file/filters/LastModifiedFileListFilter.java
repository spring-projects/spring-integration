/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * The {@link FileListFilter} implementation to filter those files which
 * {@link File#lastModified()} is less than the age in comparison
 * with the current time.
 * <p>
 * The resolution is done in seconds.
 * <p>
 * When discardCallback {@link #addDiscardCallback(Consumer)} is provided, it called for all the
 * rejected files.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Adama Sorho
 *
 * @since 4.2
 *
 */
public class LastModifiedFileListFilter extends AbstractLastModifiedFileListFilter<File> {

	public LastModifiedFileListFilter() {
		super();
	}

	/**
	 * Construct a {@link LastModifiedFileListFilter} instance with provided age.
	 * Defaults to 60 seconds.
	 * @param age the age in seconds.
	 * @since 5.0
	 */
	public LastModifiedFileListFilter(long age) {
		super(Duration.ofSeconds(age));
	}

	@Override
	protected Instant getLastModified(File file) {
		return Instant.ofEpochSecond(file.lastModified() / ONE_SECOND);
	}

}
