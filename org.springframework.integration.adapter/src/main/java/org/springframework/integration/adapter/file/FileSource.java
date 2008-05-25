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
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Source;
import org.springframework.util.Assert;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSource extends AbstractDirectorySource implements Source<Object>, InitializingBean, MessageDeliveryAware {

	private final File directory;

	private volatile MessageCreator<File, ?> messageCreator;

	private volatile FileFilter fileFilter;

	private volatile FilenameFilter filenameFilter;


	public FileSource(File directory) {
		this(directory, new FileMessageCreator());
	}

	public FileSource(File directory, MessageCreator<File, ?> messageCreator) {
		super(messageCreator);
		Assert.notNull(directory, "The directory must not be null");
		this.directory = directory;
	}


	public void setMessageCreator(MessageCreator<File, ?> messageCreator) {
		this.messageCreator = messageCreator;
	}

	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	public void afterPropertiesSet() {
		if (null == this.messageCreator) {
			this.messageCreator = new FileMessageCreator();
		}
	}

	@Override
	protected void disconnect() {
		// No action is necessary
	}

	@Override
	protected void establishConnection() throws IOException {
		// No action is necessary
	}

	@Override
	protected void populateSnapshot(Map<String, FileInfo> snapshot) throws IOException {
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
			FileInfo fileInfo = new FileInfo(files[i].getName(), files[i].lastModified(), files[i].length());
			snapshot.put(files[i].getName(), fileInfo);
		}
	}

	@Override
	protected File retrieveNextFile() throws IOException {
		String fileName = this.getDirectoryContentManager().getBacklog().keySet().iterator().next();
		return new File(directory, fileName);
	}

}
