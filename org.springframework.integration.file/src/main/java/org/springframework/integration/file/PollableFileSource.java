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

package org.springframework.integration.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * PollableSource that creates messages from a file system directory. To prevent
 * messages for certain files, you may supply a {@link FileListFilter}. By
 * default, an {@link AcceptOnceFileListFilter} is used. It ensures files are picked
 * up only once from the directory.
 * <p>
 * A common problem with reading files is that a file may be detected before it
 * is ready. The default {@link AcceptOnceFileListFilter} does not prevent this. In
 * most cases, this can be prevented if the file-writing process renames each
 * file as soon as it is ready for reading. A pattern-matching filter that
 * accepts only files that are ready (e.g. based on a known suffix), composed
 * with the default {@link AcceptOnceFileListFilter} would allow for this.
 * See {@ link CompositeFileFilter} for a way to do this.
 * 
 * @author Iwein Fuld
 */
public class PollableFileSource implements MessageSource<File>, MessageDeliveryAware<File> {

	private static final Log logger = LogFactory.getLog(PollableFileSource.class);

	private volatile File inputDirectory;

	private final Queue<File> toBeReceived = new PriorityBlockingQueue<File>();

	private volatile FileListFilter filter = new AcceptOnceFileListFilter();


	public void setInputDirectory(Resource inputDirectory) {
		Assert.notNull(inputDirectory, "inputDirectory cannot be null");
		Assert.isTrue(inputDirectory.exists(), inputDirectory + " doesn't exist.");
		try {
			this.inputDirectory = inputDirectory.getFile();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unexpected IOException when looking for " + inputDirectory, e);
		}
		Assert.isTrue(this.inputDirectory.canRead(), "No read permissions on " + this.inputDirectory);
	}

	/**
	 * Sets a {@link FileFilter} on the {@link PollableSource}. By default a
	 * {@link AcceptOnceFileListFilter} with no bounds is used. In most cases a
	 * customized {@link FileFilter} will be needed to deal with modification
	 * and duplication concerns. If multiple filters are required a
	 * {@link CompositeFileListFilter} can be used to group them together <p/>
	 * <b>Note that the supplied filter must be thread safe</b>.
	 */
	public void setFilter(FileListFilter filter) {
		Assert.notNull(filter, "'filter' should not be null");
		this.filter = filter;
	}

	public Message<File> receive() throws MessagingException {
		refreshQueue();
		Message<File> message = null;
		File file = toBeReceived.poll();
		// we can't rely on isEmpty for concurrency reasons
		if (file != null) {
			message = new GenericMessage<File>(file);
			if (logger.isInfoEnabled()) {
				logger.info("Created message: [" + message + "]");
			}
		}
		return message;
	}

	private void refreshQueue() {
		List<File> filteredFiles = filter.filterFiles((inputDirectory.listFiles()));
		Set<File> freshFiles = new HashSet<File>(filteredFiles);
		if (!freshFiles.isEmpty()) {
			// don't duplicate what's on the queue already
			freshFiles.removeAll(toBeReceived);
			toBeReceived.addAll(freshFiles);
			if (logger.isDebugEnabled()) {
				logger.debug("Added to queue: " + freshFiles);
			}
		}
	}

	/**
	 * In concurrent scenarios onFailure() might cause failing files to be
	 * ignored. If this is not acceptable access to this method should be
	 * synchronized on this instance externally.
	 */
	public void onFailure(Message<File> failedMessage, Throwable t) {
		if (logger.isWarnEnabled()) {
			logger.warn("Failed to send: " + failedMessage);
		}
		toBeReceived.add(failedMessage.getPayload());
	}

	public void onSend(Message<File> sentMessage) {
		if (logger.isDebugEnabled()) {
			logger.debug("Sent: " + sentMessage);
		}
	}
}
