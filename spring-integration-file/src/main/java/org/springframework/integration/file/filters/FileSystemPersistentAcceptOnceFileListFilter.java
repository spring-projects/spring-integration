/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;

import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
public class FileSystemPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<File> {

	public FileSystemPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(File file) {
		return file.lastModified();
	}

	@Override
	protected String fileName(File file) {
		return file.getAbsolutePath();
	}

	/**
	 * Check that the file still exists, to avoid a race condition when multi-threaded and
	 * another thread removed the file while we were waiting for the lock.
	 * @since 4.3.19
	 */
	@Override
	protected boolean fileStillExists(File file) {
		return file.exists();
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
