/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp.filters;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractRecentFileListFilter;

/**
 * The {@link AbstractRecentFileListFilter} implementation for FTP protocol.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class FtpRecentFileListFilter extends AbstractRecentFileListFilter<FTPFile> {

	public FtpRecentFileListFilter() {
		super();
	}

	public FtpRecentFileListFilter(Duration age) {
		super(age);
	}

	@Override
	protected Instant getLastModified(FTPFile remoteFile) {
		return remoteFile.getTimestampInstant();
	}

}
