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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Source;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for implementing a Source that creates messages from files in a directory,
 * either local or remote.
 * 
 * @author Marius Bogoevici
 */
public abstract class AbstractDirectorySource implements Source<Object>, MessageDeliveryAware {

	public final static String FILE_INFO_PROPERTY = "file.info";


	private final Log logger = LogFactory.getLog(this.getClass());

	private final DirectoryContentManager directoryContentManager = new DirectoryContentManager();

	private final MessageCreator<File, ?> messageCreator;


	public AbstractDirectorySource(MessageCreator<File, ?> messageCreator) {
		Assert.notNull(messageCreator, "The MessageCreator must not be null");
		this.messageCreator = messageCreator;
	}


	protected DirectoryContentManager getDirectoryContentManager() {
		return this.directoryContentManager;
	}

	public MessageCreator<File, ?> getMessageCreator() {
		return messageCreator;
	}

	public final Message receive() {
		try {
			this.establishConnection();
			HashMap<String, FileInfo> snapshot = new HashMap<String, FileInfo>();
			this.populateSnapshot(snapshot);
			this.directoryContentManager.processSnapshot(snapshot);
			if (!getDirectoryContentManager().getBacklog().isEmpty()) {
				File file = retrieveNextFile();
				Message message = this.messageCreator.createMessage(file);
				message.getHeader().setProperty(FileNameGenerator.FILENAME_PROPERTY_KEY, file.getName());
				message.getHeader().setAttribute(FILE_INFO_PROPERTY,
						getDirectoryContentManager().getBacklog().get(file.getName()));
				return message;
			}
			return null;
		}
		catch (Exception e) {
			throw new MessagingException("Error while polling for messages.", e);
		}
		finally {
			disconnect();
		}
	}

	public void onSend(Message<?> message) {
		String filename = message.getHeader().getProperty(FileNameGenerator.FILENAME_PROPERTY_KEY);
		if (StringUtils.hasText(filename)) {
			this.directoryContentManager.fileProcessed(filename);
		}
		else if (this.logger.isWarnEnabled()) {
			logger.warn("No filename in Message header, cannot send notification of processing.");
		}
	}

	public void onFailure(MessagingException exception) {
		if (this.logger.isWarnEnabled()) {
			logger.warn("Failure notification received by " + this.getClass().getSimpleName(), exception);
		}
	}


	/**
	 * Connects to the directory, if necessary.
	 */
	protected abstract void establishConnection() throws IOException;

	/**
	 * Constructs the snapshot by iterating files.
	 * @param snapshot
	 * @throws IOException
	 */
	protected abstract void populateSnapshot(Map<String, FileInfo> snapshot) throws IOException;

	/**
	 * Returns the next file, based on the backlog data.
	 * @return
	 * @throws IOException
	 */
	protected abstract File retrieveNextFile() throws IOException;

	/**
	 * Disconnects from the directory
	 */
	protected abstract void disconnect();

}
