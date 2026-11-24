/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.sftp.filters;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * Implementation of {@link AbstractSimplePatternFileListFilter} for SFTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<SftpClient.DirEntry> {

	public SftpSimplePatternFileListFilter(String pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(SftpClient.DirEntry entry) {
		return entry.getFilename();
	}

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file.getAttributes().isDirectory();
	}

}
