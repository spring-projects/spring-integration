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

package org.springframework.integration.adapter.file;

import java.io.File;

import org.springframework.util.Assert;

/**
 * Information about a file.
 * 
 * The FileSnapshot takes a snapshot of certain mutable properties of a file and
 * stores them in an immutable way. This can be useful to determine if files
 * have been changed.
 * 
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class FileSnapshot implements Comparable<FileSnapshot> {

	private final File file;

	private final long modificationTimestamp;

	private final long size;

	public FileSnapshot(File file) {
		Assert.notNull(file, "Can't take a snapshot of file that is null");
		this.file = file;
		this.modificationTimestamp = file.lastModified();
		this.size = file.length();
	}

	public FileSnapshot(String fileName, long modificationTimestamp, long size) {
		this.modificationTimestamp = modificationTimestamp;
		this.size = size;
		this.file = new File(fileName);
	}

	public String getFileName() {
		// this could be cached for better performance
		return file.getName();
	}

	public long getModificationTimestamp() {
		return modificationTimestamp;
	}

	public long getSize() {
		return size;
	}

	/**
	 * <p>
	 * Be careful to note that the file that this snapshot refers to might have
	 * changed. In particular: <code>
	 * snapshot.getModificationTimestamp() != snapshot.getFile().lastModified()
	 * </code> will evalutate to <code>true</code> in
	 * many scenarios.
	 * 
	 * @return the file that the snapshot was based on.
	 */
	public File getFile() {
		return file;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof FileSnapshot)) {
			return false;
		}
		FileSnapshot otherInfo = (FileSnapshot) other;
		return this.getSize() == otherInfo.getSize()
				&& this.getModificationTimestamp() == otherInfo.getModificationTimestamp()
				&& this.file.getName().equals(otherInfo.getFileName());
	}

	@Override
	public int hashCode() {
		return file.getPath().hashCode() ^ new Long(modificationTimestamp).hashCode() ^ new Long(size).hashCode();
	}

	public int compareTo(FileSnapshot other) {
		return this.getFile().compareTo(other.getFile());
	}

}
