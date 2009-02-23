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
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.integration.aggregator.Resequencer;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.util.Assert;

/**
 * {@link MessageSource} that creates messages from a file system directory. To
 * prevent messages for certain files, you may supply a {@link FileListFilter}.
 * By default, an {@link AcceptOnceFileListFilter} is used. It ensures files are
 * picked up only once from the directory.
 * <p/>
 * A common problem with reading files is that a file may be detected before it
 * is ready. The default {@link AcceptOnceFileListFilter} does not prevent this.
 * In most cases, this can be prevented if the file-writing process renames each
 * file as soon as it is ready for reading. A pattern-matching filter that
 * accepts only files that are ready (e.g. based on a known suffix), composed
 * with the default {@link AcceptOnceFileListFilter} would allow for this. See
 * {@link CompositeFileFilter} for a way to do this.
 * <p/>
 * A {@link Comparator} can be used to ensure internal ordering of the Files in
 * a {@link PriorityBlockingQueue}. This does not provide the same guarantees as
 * a {@link Resequencer}, but in cases where writing files and failure
 * downstream are rare it might be sufficient.
 * <p/>
 * FileReadingMessageSource is fully thread-safe under concurrent
 * <code>receive()</code> invocations and message delivery callbacks.
 * 
 * @author Iwein Fuld
 */
public class FileReadingMessageSource implements MessageSource<File> {

	private static final int INTERNAL_QUEUE_CAPACITY = 5;

	private static final Log logger = LogFactory.getLog(FileReadingMessageSource.class);

	private volatile File inputDirectory;

	/**
	 * {@link PriorityBlockingQueue#iterator()} throws
	 * {@link ConcurrentModificationException} in Java 5. There is no locking
	 * around the queue, so there is also no iteration.
	 */
	private final Queue<File> toBeReceived;

	private volatile FileListFilter filter = new AcceptOnceFileListFilter();

	private boolean scanEachPoll = false;

	/**
	 * Creates a FileReadingMessageSource with a naturally ordered queue.
	 */
	public FileReadingMessageSource() {
		toBeReceived = new PriorityBlockingQueue<File>(INTERNAL_QUEUE_CAPACITY);
	}

	/**
	 * Creates a FileReadingMessageSource with a {@link PriorityBlockingQueue}
	 * ordered with the passed in {@link Comparator}
	 * 
	 * No guarantees about file delivery order can be made under concurrent
	 * access.
	 */
	public FileReadingMessageSource(Comparator<File> receptionOrderComparator) {
		toBeReceived = new PriorityBlockingQueue<File>(INTERNAL_QUEUE_CAPACITY, receptionOrderComparator);
	}

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
	 * Sets a {@link FileListFilter}. By default a
	 * {@link AcceptOnceFileListFilter} with no bounds is used. In most cases a
	 * customized {@link FileListFilter} will be needed to deal with
	 * modification and duplication concerns. If multiple filters are required a
	 * {@link CompositeFileListFilter} can be used to group them together
	 * <p/>
	 * <b>The supplied filter must be thread safe.</b>.
	 */
	public void setFilter(FileListFilter filter) {
		Assert.notNull(filter, "'filter' should not be null");
		this.filter = filter;
	}

	/**
	 * Optional. Set this flag if you want to make sure the internal queue is
	 * refreshed with the latest content of the input directory on each poll.
	 * <p/>
	 * By default this implementation will empty its queue before looking at the
	 * directory again. In cases where order
	 */
	public void setScanEachPoll(boolean scanEachPoll) {
		this.scanEachPoll = scanEachPoll;
	}

	public Message<File> receive() throws MessagingException {
		Message<File> message = null;
		// rescan only if needed or explicitly configured
		if (scanEachPoll || toBeReceived.isEmpty()) {
			scanInputDirectory();
		}
		File file = toBeReceived.poll();
		// we can't rely on isEmpty for concurrency reasons
		if (file != null) {
			message = MessageBuilder.withPayload(file).build();
			if (logger.isInfoEnabled()) {
				logger.info("Created message: [" + message + "]");
			}
		}
		return message;
	}

	private void scanInputDirectory() {
		List<File> filteredFiles = filter.filterFiles((inputDirectory.listFiles()));
		Set<File> freshFiles = new HashSet<File>(filteredFiles);
		if (!freshFiles.isEmpty()) {
			toBeReceived.addAll(freshFiles);
			if (logger.isDebugEnabled()) {
				logger.debug("Added to queue: " + freshFiles);
			}
		}
	}

	/**
	 * Adds the failed message back to the 'toBeReceived' queue.
	 */
	public void onFailure(Message<File> failedMessage, Throwable t) {
		if (logger.isWarnEnabled()) {
			logger.warn("Failed to send: " + failedMessage);
		}
		toBeReceived.offer(failedMessage.getPayload());
	}

	/**
	 * The message is just logged. It was already removed from the queue during
	 * the call to <code>receive()</code>
	 */
	public void onSend(Message<File> sentMessage) {
		if (logger.isDebugEnabled()) {
			logger.debug("Sent: " + sentMessage);
		}
	}
}
