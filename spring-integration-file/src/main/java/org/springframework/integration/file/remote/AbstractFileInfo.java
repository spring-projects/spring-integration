/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.integration.json.SimpleJsonSerializer;

/**
 * Abstract implementation of {@link FileInfo}; provides a setter
 * for the remote directory and a generic toString implementation.
 *
 * @param <F> The target protocol file type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractFileInfo<F> implements FileInfo<F>, Comparable<FileInfo<F>> {

	private String remoteDirectory;

	/**
	 * @param remoteDirectory the remoteDirectory to set
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	public String getRemoteDirectory() {
		return this.remoteDirectory;
	}

	@Override
	public int compareTo(FileInfo<F> o) {
		return this.getFilename().compareTo(o.getFilename());
	}

	public String toJson() {
		return SimpleJsonSerializer.toJson(this, "fileInfo");
	}

	@Override
	public String toString() {
		return "FileInfo [isLink=" + isLink()
				+ ", Filename=" + getFilename()
				+ ", RemoteDirectory=" + getRemoteDirectory() + "]";
	}

}
