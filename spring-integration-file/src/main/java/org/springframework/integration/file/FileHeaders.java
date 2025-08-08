/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

/**
 * Pre-defined header names to be used when storing or retrieving
 * File-related values to/from integration Message Headers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class FileHeaders {

	public static final String PREFIX = "file_";

	public static final String FILENAME = PREFIX + "name";

	public static final String RELATIVE_PATH = PREFIX + "relativePath";

	public static final String ORIGINAL_FILE = PREFIX + "originalFile";

	public static final String REMOTE_DIRECTORY = PREFIX + "remoteDirectory";

	public static final String REMOTE_FILE = PREFIX + "remoteFile";

	public static final String RENAME_TO = PREFIX + "renameTo";

	public static final String SET_MODIFIED = PREFIX + "setModified";

	/**
	 * Record is a file marker (START/END).
	 */
	public static final String MARKER = PREFIX + "marker";

	/**
	 * The line count for END marker message after splitting.
	 */
	public static final String LINE_COUNT = PREFIX + "lineCount";

	/**
	 * A remote file information representation.
	 */
	public static final String REMOTE_FILE_INFO = PREFIX + "remoteFileInfo";

	/**
	 * A remote host/port the file has been polled from.
	 */
	public static final String REMOTE_HOST_PORT = PREFIX + "remoteHostPort";

}
