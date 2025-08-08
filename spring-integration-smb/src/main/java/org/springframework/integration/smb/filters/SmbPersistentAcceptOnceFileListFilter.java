/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.smb.filters;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * Implementation of {@link AbstractPersistentAcceptOnceFileListFilter} for SMB.
 *
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<SmbFile> {

	public SmbPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(SmbFile file) {
		return file.getLastModified();
	}

	@Override
	protected String fileName(SmbFile file) {
		return file.getName();
	}

}
