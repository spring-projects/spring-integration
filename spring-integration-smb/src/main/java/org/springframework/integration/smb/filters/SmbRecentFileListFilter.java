/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import java.time.Duration;
import java.time.Instant;

import org.codelibs.jcifs.smb.impl.SmbFile;

import org.springframework.integration.file.filters.AbstractRecentFileListFilter;

/**
 * The {@link AbstractRecentFileListFilter} implementation for SMB protocol.
 *
 * @author Artem Bilan
 * @author Daniel Frey
 *
 * @since 6.2
 */
public class SmbRecentFileListFilter extends AbstractRecentFileListFilter<SmbFile> {

	public SmbRecentFileListFilter() {
		super();
	}

	public SmbRecentFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(SmbFile remoteFile) {
		return Instant.ofEpochSecond(remoteFile.getLastModified() / ONE_SECOND);
	}

}
