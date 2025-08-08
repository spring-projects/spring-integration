/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import java.io.UncheckedIOException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * Implementation of {@link AbstractSimplePatternFileListFilter} for SMB.
 *
 * @author Markus Spann
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<SmbFile> {

	public SmbSimplePatternFileListFilter(String pathPattern) {
		super(pathPattern);
	}

	/**
	 * Gets the specified SMB file's name.
	 * @param file SMB file object
	 * @return file name
	 * @see AbstractSimplePatternFileListFilter#getFilename(java.lang.Object)
	 */
	@Override
	protected String getFilename(SmbFile file) {
		return (file != null) ? file.getName() : null;
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file.isDirectory();
		}
		catch (SmbException e) {
			throw new UncheckedIOException(e);
		}
	}

}
