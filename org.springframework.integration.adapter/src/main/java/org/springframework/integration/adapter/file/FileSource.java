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
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A messaging source that polls a directory to retrieve files.
 * 
 * @deprecated Replaced by org.springframework.integration.file.PollableFileSource.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class FileSource extends AbstractDirectorySource<File> implements MessageDeliveryAware<File> {

	private final File directory;

	private volatile FileFilter fileFilter;

	private volatile FilenameFilter filenameFilter;

	/**
	 * Creates a default FileSource on the specified directory
	 * @param directory
	 */
	public FileSource(Resource directory) {
		this(directory, new FileMessageCreator(), null);
	}

	public FileSource(Resource directory, Comparator<FileSnapshot> comparator) {
		this(directory, new FileMessageCreator(), comparator);
	}

	public FileSource(Resource directory, MessageCreator<File, File> messageCreator) {
		this(directory, messageCreator, null);
	}

	/**
	 * Creates a FileSource with the specified strategies.
	 * @param directory
	 * @param messageCreator the MessageCreator used to convert Files into
	 * Messages
	 * @param comparator the comparator is used to order the backlog. If
	 * <code>null</code> natural order is used.
	 */
	public FileSource(Resource directory, MessageCreator<File, File> messageCreator, Comparator<FileSnapshot> comparator) {
		super(messageCreator, comparator);
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

	@Override
	protected Message<File> buildNextMessage() throws IOException {
		File file = retrieveNextPayload();
		Message<File> message = this.getMessageCreator().createMessage(file);
		message = MessageBuilder.fromMessage(message)
				.setHeader(FileNameGenerator.FILENAME_PROPERTY_KEY, file.getName()).setHeader(FILE_INFO_PROPERTY,
						getBacklog().getProcessingBuffer().get(0)).build();
		return message;
	}

	/**
	 * Sets a FilenameFilter to be used with the
	 * <code>{@link File#listFiles()}</code> command. Note that either a
	 * FileFilter or a FilenameFilter is used to filter the list of files.
	 * Calling this setter overwrites the FileNameFilter if it was set before.
	 * @param fileFilter
	 */
	public void setFileFilter(FileFilter fileFilter) {
		Assert.notNull(fileFilter);
		this.filenameFilter = null;
		this.fileFilter = fileFilter;
	}

	/**
	 * Sets a FilenameFilter to be used with the
	 * <code>{@link File#listFiles()}</code> command. Note that either a
	 * FileFilter or a FilenameFilter is used to filter the list of files.
	 * Calling this setter overwrites the FileFilter if it was set before.
	 * @param filenameFilter
	 */
	public void setFilenameFilter(FilenameFilter filenameFilter) {
		Assert.notNull(filenameFilter);
		this.fileFilter = null;
		this.filenameFilter = filenameFilter;
	}

	@Override
	protected void populateSnapshot(List<FileSnapshot> snapshot) throws IOException {
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
			throw new MessagingException("Problem occurred while polling for files. " + "Is '"
					+ directory.getAbsolutePath() + "' a directory?");
		}
		for (File file : files) {
			FileSnapshot fileInfo = new FileSnapshot(file);
			snapshot.add(fileInfo);
		}
	}

	@Override
	protected File retrieveNextPayload() throws IOException {
		List<FileSnapshot> selectedForProcessing = this.getBacklog().selectForProcessing(1);
		if (!selectedForProcessing.isEmpty()) {
			return selectedForProcessing.get(0).getFile();
		}
		else {
			return null;
		}
	}

}
