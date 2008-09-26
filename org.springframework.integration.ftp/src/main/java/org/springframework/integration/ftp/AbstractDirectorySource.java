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

package org.springframework.integration.ftp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessagingException;

/**
 * Base class for implementing a PollableSource that creates messages from files
 * in a directory, either local or remote.
 * 
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public abstract class AbstractDirectorySource<T> implements MessageSource<T>, MessageDeliveryAware<T> {

	public final static String FILE_INFO_PROPERTY = "file.info";

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Backlog<FileSnapshot> backlog;


	public AbstractDirectorySource() {
		this(null);
	}

	public AbstractDirectorySource(Comparator<FileSnapshot> comparator) {
		this.backlog = comparator == null ? new Backlog<FileSnapshot>() : new Backlog<FileSnapshot>(comparator);
	}


	protected Backlog<FileSnapshot> getBacklog() {
		return this.backlog;
	}

	public final Message<T> receive() {
		try {
			refreshSnapshotAndMarkProcessing(this.backlog);
			if (!getBacklog().isEmpty()) {
				return buildNextMessage();
			}
			return null;
		}
		catch (Exception e) {
			throw new MessagingException("Error while polling for messages.", e);
		}
	}

	protected void refreshSnapshotAndMarkProcessing(Backlog<FileSnapshot> backlog) throws IOException {
		List<FileSnapshot> snapshot = new ArrayList<FileSnapshot>();
		this.populateSnapshot(snapshot);
		backlog.processSnapshot(snapshot);
	}

	/**
	 * Hook point for implementors to create the next message that should be
	 * received. Implementations can use a File by File approach or in cases
	 * where retrieval could be expensive because of network latency, a batched
	 * approach could be implemented here. See FtpSource for an example.
	 * 
	 * @return the next message containing (part of) the unprocessed content of
	 * the directory
	 * @throws IOException
	 */
	protected Message<T> buildNextMessage() throws IOException {
		return MessageBuilder.withPayload(retrieveNextPayload()).build();
	}

	public void onSend(Message<T> message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message + " processed successfully. Files will be removed from backlog");
		}
		this.backlog.processed();
	}

	public void onFailure(Message<T> failedMessage, Throwable exception) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn("Failure notification received by [" + this.getClass().getSimpleName() + "] for message: "
					+ failedMessage + ". Selected files will be moved back to the backlog.", exception);
		}
		this.backlog.processingFailed();
	}

	/**
	 * Constructs the snapshot by iterating files.
	 */
	protected abstract void populateSnapshot(List<FileSnapshot> snapshot) throws IOException;

	/**
	 * Returns the next file, based on the backlog data.
	 */
	protected abstract T retrieveNextPayload() throws IOException;

	protected final void filesProcessed() {
		this.backlog.processed();
	}

}
