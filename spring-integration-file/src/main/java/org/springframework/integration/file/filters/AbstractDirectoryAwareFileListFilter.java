/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.file.filters;

/**
 * A file list filter that can be configured to always accept (pass) directories.
 * This permits, for example, pattern matching on just files when using recursion
 * to examine a directory tree.
 *
 * @param <F> the file type.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public abstract class AbstractDirectoryAwareFileListFilter<F> extends AbstractFileListFilter<F> {

	private boolean alwaysAcceptDirectories;

	private boolean forRecursion;

	/**
	 * Set to true so that filters that support this feature can unconditionally pass
	 * directories; default false.
	 * @param alwaysAcceptDirectories true to always pass directories.
	 */
	public void setAlwaysAcceptDirectories(boolean alwaysAcceptDirectories) {
		this.alwaysAcceptDirectories = alwaysAcceptDirectories;
	}

	@Override
	public boolean isForRecursion() {
		return this.forRecursion;
	}

	/**
	 * Set to true to inform a recursive gateway operation to use the full file path as
	 * the metadata key. Also sets {@link #alwaysAcceptDirectories}.
	 * @param forRecursion true to use the full path.
	 * @since 5.3.6
	 */
	public void setForRecursion(boolean forRecursion) {
		this.forRecursion = forRecursion;
		this.alwaysAcceptDirectories = forRecursion;
	}

	protected boolean alwaysAccept(F file) {
		return file != null && this.alwaysAcceptDirectories && isDirectory(file);
	}

	/**
	 * Subclasses must implement this method to indicate whether the file
	 * is a directory or not.
	 * @param file the file.
	 * @return true if it's a directory.
	 */
	protected abstract boolean isDirectory(F file);

}
