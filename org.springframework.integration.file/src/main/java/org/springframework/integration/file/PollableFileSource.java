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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * PollableSource that creates messages from a file system directory. To prevent
 * messages from showing up on the source you can supply a FileFilter to it.
 * This can also be useful to prevent messages to be created for unfinished
 * files.
 * 
 * @author Iwein Fuld
 * 
 * @param <T> the class of the payload of the message received from this
 * {@link #PollableFileSource()}
 */
public class PollableFileSource<T> implements PollableSource<T>, MessageDeliveryAware, InitializingBean {

	private static Log log = LogFactory.getLog(PollableFileSource.class);

	private volatile Queue<File> fileQueue = new PriorityBlockingQueue<File>();

	private volatile MessageCreator<File, T> messageCreator;

	private volatile File inputDirectory;

	private final Map<Message<T>, File> messagesInFlight = new ConcurrentHashMap<Message<T>, File>();

	private final AtomicLong currentListTimestamp = new AtomicLong();

	private final AtomicLong previousListTimestamp = new AtomicLong();

	private volatile CompositeFileFilter filter = new CompositeFileFilter(new ModificationTimeFileFilter());

	// Setters
	public void setQueue(Queue<File> queue) {
		this.fileQueue = queue;
	}

	public void setMessageCreator(MessageCreator<File, T> messageCreator) {
		this.messageCreator = messageCreator;
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public void setFilter(FileFilter... filters) {
		Assert.notEmpty(filters);
		this.filter = filter.addFilter(filters);
	}

	public void afterPropertiesSet() throws Exception {
		if (this.messageCreator == null) {
			throw new ConfigurationException(MessageCreator.class.getSimpleName() + "is required.");
		}
		if (this.inputDirectory == null) {
			throw new ConfigurationException("inputDirectory cannot be null");
		}
		if (!this.inputDirectory.exists()) {
			throw new ConfigurationException(inputDirectory + " doesn't exist.");
		}
		if (!this.inputDirectory.canRead()) {
			throw new ConfigurationException("No read permissions on " + inputDirectory);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return the Message created by the {@link #messageCreator} based on the
	 * next file from the {@link #fileQueue}. If the file doesn't exist
	 * (anymore) it is up to the {@link MessageCreator} to deal with this.
	 */
	public Message<T> receive() throws MessagingException {
		traceState();
		refreshQueue();
		Message<T> message = null;
		File file = fileQueue.poll();
		// we cannot rely on isEmpty, so we have to do a null check
		if (file != null) {
			message = createAndTrackMessage(file);
			if (log.isInfoEnabled()) {
				log.info("Created message: [" + message + "]");
			}
		}
		traceState();
		return message;
	}

	private Message<T> createAndTrackMessage(File file) throws MessagingException {
		if (log.isDebugEnabled()) {
			log.debug("Preparing message for file: [" + file + "]");
		}
		Message<T> message;
		try {
			message = messageCreator.createMessage(file);
			messagesInFlight.put(message, file);
		}
		catch (Exception e) {
			fileQueue.add(file);
			throw new MessagingException("Error creating message for file: [" + file + "]", e);
		}
		return message;
	}

	private void refreshQueue() {
		File[] freshFiles = getFreshFilesAndIncrementTimestamp();
		if (!ObjectUtils.isEmpty(freshFiles)) {
			List<File> freshFilesList = new ArrayList<File>(Arrays.asList(freshFiles));
			// don't duplicate what's on the queue already
			freshFilesList.removeAll(fileQueue);
			freshFilesList.removeAll(messagesInFlight.values());
			fileQueue.addAll(freshFilesList);
			if (log.isDebugEnabled()) {
				log.debug("Added to queue: " + freshFilesList);
			}
		}
	}

	/*
	 * This is synchronized on this instance to prevent concurrent listings from
	 * causing duplication.
	 * 
	 * All filesystems provide a modification time precision to the second, so
	 * we allow at most one refresh per second.
	 */
	private synchronized File[] getFreshFilesAndIncrementTimestamp() {
		previousListTimestamp.set(currentListTimestamp.getAndSet(System.currentTimeMillis() / 1000 * 1000));
		File[] freshFiles = new File[] {};
		if (currentListTimestamp.get() > previousListTimestamp.get()) {
			freshFiles = inputDirectory.listFiles(filter);
		}
		return freshFiles;
	}

	/**
	 * In concurrent scenarios onFailure() might cause failing files to be
	 * ignored. If this is not acceptable access to this method should be
	 * synchronized on this instance externally.
	 */
	public void onFailure(Message<?> failedMessage, Throwable t) {
		log.warn("Failed to send: " + failedMessage);
		fileQueue.add(messagesInFlight.get(failedMessage));
		messagesInFlight.remove(failedMessage);
	}

	public void onSend(Message<?> sentMessage) {
		if (log.isDebugEnabled()) {
			log.debug("Sent: " + sentMessage);
		}
		messagesInFlight.remove(sentMessage);
	}

	/*
	 * utility method to trace the stateful collections of this instance.
	 */
	private void traceState() {
		if (log.isTraceEnabled()) {
			log.trace("Files to be received: [" + fileQueue + "]");
			log.trace("Messages in flight: [" + messagesInFlight.keySet() + "]");
		}
	}

	/*
	 * Helper to filter files based on a modification time
	 */
	private class ModificationTimeFileFilter implements FileFilter {
		public boolean accept(File file) {
			long lastModified = file.lastModified();
			return lastModified > previousListTimestamp.get() && lastModified < currentListTimestamp.get();
		}
	}
}
