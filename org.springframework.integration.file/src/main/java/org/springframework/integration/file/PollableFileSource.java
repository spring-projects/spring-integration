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
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;

/**
 * PollableSource that creates messages from a file system directory. To prevent
 * messages from showing up on the source you can supply a FileFilter to it. By
 * default an {@link AcceptOnceFileFilter} is used that ensures files are picked
 * up only once from the directory.
 * 
 * A common problem with reading files is that files are picked up that are not
 * ready. The default {@link AcceptOnceFileFilter} does not prevent this. In
 * most cases this can be prevented by renaming the files as soon as they are
 * ready. A FileFilter that accepts only files that are ready, composed with the
 * default {@link AcceptOnceFileFilter} would allow for this.
 * @see CompositeFileFilter for a way to do this.
 * 
 * @author Iwein Fuld
 */
public class PollableFileSource implements PollableSource<File>, MessageDeliveryAware<File>, InitializingBean {

	private static final Log logger = LogFactory.getLog(PollableFileSource.class);

	private volatile File inputDirectory;

	private final Queue<File> toBeReceived = new PriorityBlockingQueue<File>();

	private volatile FileListFilter filter = new AcceptOnceFileFilter();

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	/**
	 * Sets a {@link FileFilter} on the {@link PollableSource}. By default a
	 * {@link AcceptOnceFileFilter} with no bounds is used. In most cases a
	 * customized {@link FileFilter} will be needed to deal with modification
	 * and duplication concerns. If multiple filters are required a
	 * {@link CompositeFileFilter} can be used to group them together <p/>
	 * <b>Note that the supplied filter must be thread safe</b>.
	 */
	public void setFilter(FileListFilter filter) {
		Assert.notNull(filter, "'filter' should not be null");
		this.filter = filter;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(inputDirectory, "inputDirectory cannot be null");
		Assert.isTrue(this.inputDirectory.exists(), inputDirectory + " doesn't exist.");
		Assert.isTrue(this.inputDirectory.canRead(), "No read permissions on " + inputDirectory);
	}

	public Message<File> receive() throws MessagingException {
		traceState();
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
		traceState();
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

	/*
	 * utility method to trace the stateful collections of this instance.
	 */
	private void traceState() {
		if (logger.isTraceEnabled()) {
			logger.trace("Files to be received: [" + toBeReceived + "]");
		}
	}
}
