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
import java.io.FileFilter;
import java.io.FilenameFilter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @author Mark Fisher
 */
public class FileSource implements PollableSource<Object>, InitializingBean {

	private final File directory;

	private volatile boolean textBased = true;

	private volatile AbstractFileMapper<?> mapper;

	private volatile FileNameGenerator fileNameGenerator;

	private volatile FileFilter fileFilter;

	private volatile FilenameFilter filenameFilter;


	public FileSource(File directory) {
		Assert.notNull(directory, "directory must not be null");
		this.directory = directory;
	}


	public boolean isTextBased() {
		return this.textBased;
	}

	public void setTextBased(boolean textBased) {
		this.textBased = textBased;
	}

	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void afterPropertiesSet() {
		if (this.isTextBased()) {
			this.mapper = new TextFileMapper(this.directory);
		}
		else {
			this.mapper = new ByteArrayFileMapper(this.directory);
		}
		if (this.fileNameGenerator != null) {
			this.mapper.setFileNameGenerator(this.fileNameGenerator);
		}
	}

	public Message receive() {
		File[] files = null;
		if (this.fileFilter != null) {
			files = this.directory.listFiles(this.fileFilter);
		}
		else if (this.filenameFilter != null) {
			files = this.directory.listFiles(this.filenameFilter);
		}
		else {
			files = this.directory.listFiles();
		}
		if (files == null) {
			throw new MessagingException("Problem occurred while polling for files. " +
					"Is '" + directory.getAbsolutePath() + "' a directory?");
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				return this.mapper.createMessage(files[i]);
			}
		}
		return null;
	}

}
