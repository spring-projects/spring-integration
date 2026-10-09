/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

/**
 * The {@link AbstractRecentFileListFilter} implementation for local file system.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class RecentFileListFilter extends AbstractRecentFileListFilter<File> {

	public RecentFileListFilter() {
	}

	public RecentFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(File file) {
		return Instant.ofEpochSecond(file.lastModified() / ONE_SECOND);
	}

}
