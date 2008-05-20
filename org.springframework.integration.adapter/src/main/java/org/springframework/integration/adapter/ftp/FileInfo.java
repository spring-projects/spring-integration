/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.adapter.ftp;

/**
 * Information about a file in a directory.
 * 
 * @author Marius Bogoevici
 */
public class FileInfo {

	private final String fileName;

	private final long modificationTimestamp;

	private final long size;


	public FileInfo(String fileName, long modificationTimestamp, long size) {
		this.fileName = fileName;
		this.modificationTimestamp = modificationTimestamp;
		this.size = size;
	}


	public String getFileName() {
		return fileName;
	}

	public long getModificationTimestamp() {
		return modificationTimestamp;
	}

	public long getSize() {
		return size;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof FileInfo)) {
			return false;
		}
		FileInfo otherInfo = (FileInfo) other;
		return this.getSize() == otherInfo.getSize()
				&& this.getModificationTimestamp() == otherInfo.getModificationTimestamp()
				&& this.fileName.equals(otherInfo.getFileName());
	}

	@Override
	public int hashCode() {
		return (fileName == null ? 0 : fileName.hashCode()) ^ new Long(modificationTimestamp).hashCode()
				^ new Long(size).hashCode();
	}

}
