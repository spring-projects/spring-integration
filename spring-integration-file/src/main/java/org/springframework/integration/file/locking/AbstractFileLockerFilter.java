/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;

import org.springframework.integration.file.FileLocker;
import org.springframework.integration.file.filters.AbstractFileListFilter;

/**
 * Convenience base class for implementing FileLockers that check a lock before accepting a file. This is needed
 * when used in combination with a FileReadingMessageSource through a DirectoryScanner.
 *
 * @author Iwein Fuld
 * @since 2.0
 *
 */
public abstract class AbstractFileLockerFilter extends AbstractFileListFilter<File> implements FileLocker {

	@Override
	public boolean accept(File file) {
		return this.isLockable(file);
	}

}
