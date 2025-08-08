/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;
import java.util.List;

import org.springframework.integration.file.filters.FileListFilter;

/**
 * Strategy for scanning directories. Implementations may select all children
 * and grandchildren of the scanned directory in any order. This interface is
 * intended to enable the customization of selection, locking and ordering of
 * files in a directory like RecursiveDirectoryScanner. If the only requirement
 * is to ignore certain files a EntryListFilter implementation should suffice.
 *
 * @author Iwein Fuld
 */
public interface DirectoryScanner {

	/**
	 * Scans the directory according to the strategy particular to this
	 * implementation and returns the selected files as a File array. This
	 * method may never return files that are rejected by the filter.
	 *
	 * @param directory the directory to scan for files
	 * @return a list of files representing the content of the directory
	 * @throws IllegalArgumentException if the input is incorrect
	 */
	List<File> listFiles(File directory) throws IllegalArgumentException;

	/**
	 * Sets a custom filter to be used by this scanner. The filter will get a
	 * chance to reject files before the scanner presents them through its
	 * listFiles method. A scanner may use additional filtering that is out of
	 * the control of the provided filter.
	 *
	 * @param filter
	 *            the custom filter to be used
	 */
	void setFilter(FileListFilter<File> filter);

	/**
	 * Sets a custom locker to be used by this scanner. The locker will get a
	 * chance to lock files and reject claims on files that are already locked.
	 *
	 * @param locker
	 *            the custom locker to be used
	 */
	void setLocker(FileLocker locker);

	/**
	 * Claim the file to process. It is up to the implementation to decide what
	 * additional safe guards are required to attain a claim to the file. But if
	 * a locker is set implementations MUST invoke its <code>lock</code> method
	 * and MUST return <code>false</code> if the locker did not grant the lock.
	 *
	 * @param file file to be claimed
	 * @return true if the claim was granted false otherwise
	 */
	boolean tryClaim(File file);

}
