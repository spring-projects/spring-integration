/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.remote;

/**
 * Represents a remote file info - an abstraction over the underlying implementation.
 *
 * @param <F> The target protocol file type.
 *
 * @author Gary Russell
 *
 * @since 2.1
 */
public interface FileInfo<F> {

	/**
	 * @return true if the remote file is a directory
	 */
	boolean isDirectory();

	/**
	 * @return true if the remote file is a link
	 */
	boolean isLink();

	/**
	 * @return the size of the remote file
	 */
	long getSize();

	/**
	 * @return the modified time of the remote file
	 */
	long getModified();

	/**
	 * @return the name of the remote file
	 */
	String getFilename();

	/**
	 * @return the remote directory in which the file resides
	 */
	String getRemoteDirectory();

	/**
	 * @return a string representing the permissions of the remote
	 * file (e.g. -rw-r--r--).
	 */
	String getPermissions();

	/**
	 * @return the actual implementation from the underlying
	 * library, more sophisticated access is needed.
	 */
	F getFileInfo();

}
