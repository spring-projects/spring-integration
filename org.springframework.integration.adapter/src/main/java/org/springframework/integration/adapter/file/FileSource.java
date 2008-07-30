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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSource extends AbstractDirectorySource<File> implements MessageDeliveryAware {

	private final Log logger = LogFactory.getLog(this.getClass());
	
	private final File directory;

	private volatile FileFilter fileFilter;

	private volatile FilenameFilter filenameFilter;


	public FileSource(Resource directory) {
		this(directory, new FileMessageCreator());
	}
	
	@Override
	protected Message<File> buildNextMessage() throws IOException {
		File file = retrieveNextPayload();
		Message<File> message = this.getMessageCreator().createMessage(file);
		message = MessageBuilder.fromMessage(message)
				.setHeader(FileNameGenerator.FILENAME_PROPERTY_KEY, file.getName()).setHeader(FILE_INFO_PROPERTY,
						getDirectoryContentManager().getBacklog().get(file.getName())).build();
		return message;
	}

	public FileSource(Resource directory, MessageCreator<File, File> messageCreator) {
		super(messageCreator);
		Assert.notNull(directory, "The directory must not be null");
		try {
			this.directory = directory.getFile();
			if (!this.directory.isDirectory()) {
				throw new ConfigurationException("The FileSource can't be instantiated because "
						+ this.directory.getAbsolutePath() + " is not a directory.");
			}
		}
		catch (IOException e) {
			throw new ConfigurationException("The FileSource can't be instantiated", e);
		}
	}


	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void setFilenameFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	@Override
	protected void populateSnapshot(Map<String, FileInfo> snapshot) throws IOException {
		File[] files;
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
		for (File file : files) {
			FileInfo fileInfo = new FileInfo(file.getName(), file.lastModified(), file.length());
			snapshot.put(file.getName(), fileInfo);
		}
	}

	@Override
	protected File retrieveNextPayload() throws IOException {
		String fileName = this.getDirectoryContentManager().getBacklog().keySet().iterator().next();
		return new File(directory, fileName);
	}

	@Override
	public void onSend(Message<?> message) {
		String filename = message.getHeaders().get(FileNameGenerator.FILENAME_PROPERTY_KEY, String.class);
		if (StringUtils.hasText(filename)) {
			fileProcessed(filename);
		}
		else if (this.logger.isWarnEnabled()) {
			logger.warn("No filename in Message header, cannot send notification of processing.");
		}
	}
}
