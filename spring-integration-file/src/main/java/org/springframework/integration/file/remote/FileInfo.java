/*
 * Copyright 2002-2021 the original author or authors.
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
