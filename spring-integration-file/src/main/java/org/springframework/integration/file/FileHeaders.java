/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * Record is a file marker (START/END)
	 */
	public static final String MARKER = PREFIX + "marker";

	/**
	 * The line count for END marker message after splitting
	 */
	public static final String LINE_COUNT = PREFIX + "lineCount";

	/**
	 * A remote file information representation
	 */
	public static final String REMOTE_FILE_INFO = PREFIX + "remoteFileInfo";

	/**
	 * A remote host/port the file has been polled from
	 */
	public static final String REMOTE_HOST_PORT = PREFIX + "remoteHostPort";

}
