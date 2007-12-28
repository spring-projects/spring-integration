/*
 * Copyright 2002-2007 the original author or authors.
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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.adapter.PollableSource;
import org.springframework.util.Assert;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @author Mark Fisher
 */
public class FileSource implements PollableSource<File> {

	private File directory;

	private FileFilter fileFilter;

	private FilenameFilter filenameFilter;


	public FileSource(File directory) {
		Assert.notNull("directory must not be null");
		this.directory = directory;
	}

	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	public Collection<File> poll(int limit) {
		File[] files = null;
		if (this.fileFilter != null) {
			files = this.directory.listFiles(fileFilter);
		}
		else if (this.filenameFilter != null) {
			files = this.directory.listFiles(filenameFilter);
		}
		else {
			files = this.directory.listFiles();
		}
		if (files == null) {
			throw new MessageHandlingException("Problem occurred while polling for files. " +
					"Is '" + directory.getAbsolutePath() + "' a directory?");
		}
		int size = Math.min(limit, files.length);
		List<File> results = new ArrayList<File>(size);
		for (int i = 0; i < size; i++) {
			results.add(files[i]);
		}
		return results;
	}

}
