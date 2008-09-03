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
package org.springframework.integration.adapter.file.replacement;

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

/**
 * PollableSource that creates messages from a file system directory.
 * 
 * @author Iwein Fuld
 * 
 * @param <T>
 */
public class PollableFileSource<T> implements PollableSource<T>, MessageDeliveryAware, InitializingBean {

	private Log log = LogFactory.getLog(this.getClass());

	private Queue<File> queue = new PriorityBlockingQueue<File>();

	private MessageCreator<File, T> messageCreator;

	private File inputDirectory;

	private final Map<Message<T>, File> tracker = new ConcurrentHashMap<Message<T>, File>();

	private final AtomicLong lastListTimestamp = new AtomicLong();

	private final CompositeFileFilter filter = CompositeFileFilter.with(new ModificationTimeFileFilter(
			lastListTimestamp));

	public Message<T> receive() throws MessagingException {
		refreshQueue(queue);
		File file = queue.poll();
		// ignore files that have been deleted
		while (file != null && !file.exists()) {
			file = queue.poll();
		}
		if (file != null) {
			if (log.isInfoEnabled()) {
				log.info("preparing to send a message for file: [" + file + "]");
			}
			return createAndTrackMessage(file);
		}
		// queue is empty
		return null;
	}

	private Message<T> createAndTrackMessage(File file) throws MessagingException {
		Message<T> message;
		try {
			message = messageCreator.createMessage(file);
			tracker.put(message, file);
		}
		catch (Exception e) {
			log.warn("Error occured while attempting to create message. Requeuing file [" + file + "]", e);
			queue.add(file);
			throw new MessagingException("Error while polling for messages", e);
		}
		if (log.isDebugEnabled()) {
			log.debug("created message: [" + message + "]");
		}
		if (log.isTraceEnabled()) {
			log.trace("queue: [" + queue + "]");
			log.trace("tracker: [" + tracker.values() + "]");
		}
		return message;
	}

	private void refreshQueue(Queue<File> queue) {
		File[] freshFiles = getFreshFilesAndIncrementTimestamp();
		if (freshFiles != null) {
			List<File> freshFilesList = new ArrayList<File>(Arrays.asList(freshFiles));
			// don't duplicate what's on the queue already
			freshFilesList.removeAll(queue);
			freshFilesList.removeAll(tracker.values());
			if (log.isTraceEnabled()) {
				log.trace("queue: [" + queue + "]");
				log.trace("tracker: [" + tracker.values() + "]");
			}
			queue.addAll(freshFilesList);
			if (log.isDebugEnabled()) {
				log.debug("added to queue: " + freshFilesList);
			}
		}
	}

	/*
	 * This is synchronized to prevent concurrent listings from causing
	 * duplication.
	 */
	private synchronized File[] getFreshFilesAndIncrementTimestamp() {
		File[] freshFiles = inputDirectory.listFiles(filter);
		lastListTimestamp.set(System.currentTimeMillis());
		return freshFiles;
	}

	/**
	 * In concurrent scenarios onFailure() might cause failing files to be
	 * ignored. If this is not acceptable access to this method should be
	 * synchronized.
	 * 
	 * {@inheritDoc} 
	 */
	public void onFailure(Message<?> failedMessage, Throwable t) {
		log.warn("not sent: " + failedMessage);
		queue.add(tracker.get(failedMessage));
		tracker.remove(failedMessage);
	}

	public void onSend(Message<?> sentMessage) {
		if (log.isDebugEnabled()) {
			log.debug("sent: " + sentMessage);
		}
		tracker.remove(sentMessage);
	}

	// Setters
	public void setQueue(Queue<File> queue) {
		this.queue = queue;
	}

	public void setMessageCreator(MessageCreator<File, T> messageCreator) {
		this.messageCreator = messageCreator;
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public void setFilter(FileFilter filter) {
		this.filter.addFilter(filter);
	}

	public void afterPropertiesSet() throws Exception {
		if (this.messageCreator == null) {
			throw new ConfigurationException("I need a " + MessageCreator.class.getSimpleName()
					+ " in order to function.");
		}
		if (this.inputDirectory == null) {
			throw new ConfigurationException("I need an inputDirectory to read from.");
		}
		if (!this.inputDirectory.exists()) {
			throw new ConfigurationException(inputDirectory + " doesn't exist.");
		}
		if (!this.inputDirectory.canRead()) {
			throw new ConfigurationException("I can't read from " + inputDirectory);
		}
	}
}
