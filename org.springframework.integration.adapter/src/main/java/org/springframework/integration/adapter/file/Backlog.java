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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * Keeps track of a backlog in a threadsafe, stateful manner.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class Backlog<T extends Comparable<T>> {

	private final Log logger = LogFactory.getLog(this.getClass());

	private PriorityBlockingQueue<T> backlog = new PriorityBlockingQueue<T>();

	/**
	 * This is the storage for backlog that is being processed by a specific
	 * thread. Not initialized means that we're not in thread safe mode (just
	 * working directly on the backlog)
	 */
	private ThreadLocal<List<T>> processingBuffer = new ThreadLocal<List<T>>() {
		@Override
		protected List<T> initialValue() {
			return new ArrayList<T>();
		}
	};

	private Set<T> doneProcessing = Collections.synchronizedSet(new HashSet<T>());
	private Set<T> currentlyProcessing = Collections.synchronizedSet(new HashSet<T>());

	public synchronized void processSnapshot(List<T> currentSnapshot) {
		/*
		 * clear the threadLocal backlog. When the thread processes a new
		 * snapshot it is done with the previous message. If there are still
		 * messages in the processing buffer something is wrong because they
		 * were not processed, nor raised as failed.
		 */
		Assert.isTrue(processingBuffer.get().isEmpty(), "Processing buffer not emptied before poll.");
		// remove everything that is not in the latest snapshot from the backlog
		backlog.retainAll(currentSnapshot);
		doneProcessing.retainAll(currentSnapshot);
		// add everything that is new to the backlog preventing side effect on
		// currentSnapshot
		Collection<T> newInCurrentSnapshot = new ArrayList<T>(currentSnapshot);
		newInCurrentSnapshot.removeAll(backlog);
		newInCurrentSnapshot.removeAll(doneProcessing);
		newInCurrentSnapshot.removeAll(currentlyProcessing);
		backlog.addAll(newInCurrentSnapshot);
	}

	/**
	 * When a source is using a backlog in a single threaded context it can use
	 * this method instead of using
	 * <code>{@link #prepareForProcessing(int)}</code>. In a threaded scenario
	 * this method can be used to mark a subset of the processing buffer as
	 * processed. Calling this method in a threaded scenario without using
	 * <code>{@link #prepareForProcessing(int)}</code> will manipulate the
	 * backlog directly and allows for race conditions. This is only recommended
	 * when message duplication is not an issue.
	 * @param items the items that have been processed
	 */
	public void fileProcessed(T... items) {
		for (T item : items) {
			if (item != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing item '" + item + "' from the undo buffer. It has been processed.");
				}
				this.processingBuffer.get().remove(item);
				this.backlog.remove(item);
				this.doneProcessing.add(item);
			}
		}
	}

	/**
	 * Moves items from the backlog to a thread local processing buffer,
	 * reserving them for this thread. It is the responsibility of the reserving
	 * thread to process the items and provide feedback to the backlog through
	 * {@link #processed()} or {@link #processingFailed()}.
	 * @param maxBatchSize if -1 prepare the whole backlog.
	 */
	public void prepareForProcessing(int maxBatchSize) {
		List<T> processingBuffer = this.processingBuffer.get();
		if (maxBatchSize == -1) {
			this.backlog.drainTo(processingBuffer);
		}
		else {
			this.backlog.drainTo(processingBuffer, maxBatchSize);
		}
		currentlyProcessing.addAll(processingBuffer);
	}

	/**
	 * Convenience method that returns the items prepared for processing
	 * immediately.
	 * @param maxBatchSize
	 * @return prepared processing buffer
	 */
	public List<T> selectForProcessing(int maxBatchSize) {
		prepareForProcessing(maxBatchSize);
		return getProcessingBuffer();
	}

	/**
	 * Marks all from the processing buffer as done. Use only in combination
	 * with <code>{@link #prepareForProcessing(int)}</code>
	 */
	public void processed() {
		this.doneProcessing.addAll(this.processingBuffer.get());
		currentlyProcessing.removeAll(processingBuffer.get());
		this.processingBuffer.get().clear();
	}

	/**
	 * Marks all from the processing buffer as not done. Use only in combination
	 * with <code>{@link #prepareForProcessing(int)}</code>
	 */
	public void processingFailed() {
		if (logger.isDebugEnabled()) {
			logger.debug("Moving all items from processing buffer to backlog. Processing has failed");
		}
		List<T> processing = this.processingBuffer.get();
		currentlyProcessing.removeAll(processing);
		this.backlog.addAll(processing);
		processing.clear();
	}

	public List<T> getProcessingBuffer() {
		return Collections.unmodifiableList(this.processingBuffer.get());
	}

	/**
	 * @return <code>true</code> if both the thread local processing buffer and
	 * the backlog are empty.
	 */
	public boolean isEmpty() {
		return this.backlog.isEmpty() && this.processingBuffer.get().isEmpty();
	}

}
