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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;

/**
 * Base class for implementing a PollableSource that creates messages from files
 * in a directory, either local or remote.
 * 
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public abstract class AbstractDirectorySource<T> implements PollableSource<T>, MessageDeliveryAware {

	public final static String FILE_INFO_PROPERTY = "file.info";

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Backlog<FileSnapshot> directoryContentManager = new Backlog<FileSnapshot>();

	private final MessageCreator<T, T> messageCreator;

	public AbstractDirectorySource(MessageCreator<T, T> messageCreator) {
		Assert.notNull(messageCreator, "The MessageCreator must not be null");
		this.messageCreator = messageCreator;
	}

	protected Backlog<FileSnapshot> getBacklog() {
		return this.directoryContentManager;
	}

	public MessageCreator<T, T> getMessageCreator() {
		return this.messageCreator;
	}

	public final Message<T> receive() {
		try {
			refreshSnapshotAndMarkProcessing(this.directoryContentManager);
			if (!getBacklog().isEmpty()) {
				return buildNextMessage();
			}
			return null;
		}
		catch (Exception e) {
			throw new MessagingException("Error while polling for messages.", e);
		}
	}

	/**
	 * Naive implementation that ignores thread safety. Subclasses that want to
	 * be thread safe and use the reservation facilities of {@link Backlog}
	 * should override this method and call
	 * <code>directoryContentManager.fileProcessing(...)</code> with the
	 * appropriate arguments.
	 * 
	 * @param directoryContentManager
	 * @throws IOException
	 */
	protected void refreshSnapshotAndMarkProcessing(Backlog<FileSnapshot> directoryContentManager) throws IOException {
		List<FileSnapshot> snapshot = new ArrayList<FileSnapshot>();
		this.populateSnapshot(snapshot);
		directoryContentManager.processSnapshot(snapshot);
	}

	/**
	 * Hook point for implementors to create the next message that should be
	 * received. Implementations can use a File by File approach (like
	 * FileSource). In cases where retrieval could be expensive because of
	 * network latency, a batched approach could be implemented here. See
	 * FtpSource for an example.
	 * 
	 * @return the next message containing (part of) the unprocessed content of
	 * the directory
	 * @throws IOException
	 */
	protected Message<T> buildNextMessage() throws IOException {
		return this.messageCreator.createMessage(retrieveNextPayload());
	}

	public void onSend(Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message + " processed successfully. Files will be removed from backlog");
		}
		this.directoryContentManager.processed();
	}

	public void onFailure(Message<?> failedMessage, Throwable exception) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn("Failure notification received by [" + this.getClass().getSimpleName() + "] for message: "
					+ failedMessage + ". Selected files will be moved back to the backlog.", exception);
		}
		this.directoryContentManager.processingFailed();
	}

	/**
	 * Constructs the snapshot by iterating files.
	 * 
	 * @param snapshot
	 * @throws IOException
	 */
	protected abstract void populateSnapshot(List<FileSnapshot> snapshot) throws IOException;

	/**
	 * Returns the next file, based on the backlog data.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected abstract T retrieveNextPayload() throws IOException;

	protected final void filesProcessed() {
		this.directoryContentManager.processed();
	}
}
