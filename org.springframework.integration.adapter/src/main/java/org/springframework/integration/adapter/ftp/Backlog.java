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

package org.springframework.integration.adapter.ftp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

	private static final int INITIAL_QUEUE_CAPACITY = 5;

	private final Log logger = LogFactory.getLog(this.getClass());

	/*
	 * Backlog, doneProcessing and currentlyProcessing should be in consistent
	 * state together. To do that access to them is synchronized on this and
	 * atomic operations have been defined on this class. It is not a problem if
	 * items exist in more than one of these collections at the same time, but the
	 * item should not be removed from one collection before it is added to the
	 * next.
	 */
	private final PriorityBlockingQueue<T> backlog;

	// @GuardedBy(this)
	private Set<T> doneProcessing = new HashSet<T>();

	// @GuardedBy(this)
	private Set<T> currentlyProcessing = new HashSet<T>();

	/*
	 * This is the storage for backlog that is being processed by a specific
	 * thread.
	 */
	private ThreadLocal<List<T>> processingBuffer = new ThreadLocal<List<T>>() {
		@Override
		protected List<T> initialValue() {
			return new ArrayList<T>();
		}
	};

	/**
	 * Constructs a Backlog around a naturally ordered
	 * {@link PriorityBlockingQueue}.
	 */
	public Backlog() {
		this.backlog = new PriorityBlockingQueue<T>();
	}

	/**
	 * Constructs a backlog around a {@link PriorityBlockingQueue} that is
	 * created with the supplied comparator. For natural ordering use
	 * {@link #Backlog()}.
	 * @param comparator
	 */
	public Backlog(Comparator<? super T> comparator) {
		this.backlog = new PriorityBlockingQueue<T>(INITIAL_QUEUE_CAPACITY, comparator);
	}

	public void processSnapshot(List<T> currentSnapshot) {
		/*
		 * clear the threadLocal backlog. When the thread processes a new
		 * snapshot it is done with the previous message. If there are still
		 * messages in the processing buffer something is wrong because they
		 * were not processed, nor raised as failed.
		 */
		Assert.isTrue(processingBuffer.get().isEmpty(), "Processing buffer not emptied before poll.");
		/*
		 * compute everything that is new to the backlog preventing side effect
		 * on currentSnapshot.
		 */
		Collection<T> newInCurrentSnapshot = new ArrayList<T>(currentSnapshot);
		synchronized (this) {
			newInCurrentSnapshot.removeAll(backlog);
			newInCurrentSnapshot.removeAll(currentlyProcessing);
			newInCurrentSnapshot.removeAll(doneProcessing);

			backlog.retainAll(currentSnapshot);
			doneProcessing.retainAll(currentSnapshot);
			backlog.addAll(newInCurrentSnapshot);
		}
	}

	/**
	 * When a source is using a backlog in a single threaded context it can use
	 * this method instead of using
	 * <code>{@link #prepareForProcessing(int)}</code>. In a threaded scenario
	 * this method can be used to mark a subset of the processing buffer as
	 * processed. Calling this method in a threaded scenario without using
	 * <code>{@link #prepareForProcessing(int)}</code> will manipulate the
	 * backlog directly. In threaded scenarios a call to
	 * {@link #selectForProcessing(int)} followed by a call to
	 * {@link #processed()} is recommended.
	 * @param items the items that have been processed
	 */
	public synchronized void fileProcessed(T... items) {
		for (T item : items) {
			if (item != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing item '" + item + "' from the undo buffer. It has been processed.");
				}
				this.doneProcessing.add(item);
				this.backlog.remove(item);
				this.processingBuffer.get().remove(item);
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
		/*
		 * It is important to properly lock the access to backlog and
		 * currentlyProcessing in this case, because the removal from the
		 * backlog happens before addition to currently processed. If another
		 * thread accesses this type of state it might duplicate messages from
		 * the source into the backlog.
		 */
		synchronized (this) {
			if (maxBatchSize == -1) {
				this.backlog.drainTo(processingBuffer);
			}
			else {
				this.backlog.drainTo(processingBuffer, maxBatchSize);
			}
			currentlyProcessing.addAll(processingBuffer);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Preparing " + processingBuffer + " for processing");
		}
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
		if (logger.isDebugEnabled()) {
			logger.debug("Moving processing buffer " + processingBuffer.get() + " to doneProcessing");
		}
		synchronized (this) {
			this.doneProcessing.addAll(this.processingBuffer.get());
			currentlyProcessing.removeAll(processingBuffer.get());
		}
		this.processingBuffer.get().clear();
	}

	/**
	 * Marks all from the processing buffer as not done. Use only in combination
	 * with <code>{@link #prepareForProcessing(int)}</code>
	 */
	public void processingFailed() {
		if (logger.isDebugEnabled()) {
			logger.debug("Moving processing buffer " + processingBuffer.get()
					+ " back to backlog. Processing has failed");
		}
		List<T> processing = this.processingBuffer.get();
		synchronized (this) {
			this.backlog.addAll(processing);
			currentlyProcessing.removeAll(processing);
		}
		processing.clear();
	}

	public List<T> getProcessingBuffer() {
		return Collections.unmodifiableList(this.processingBuffer.get());
	}

	/**
	 * Asks the backlog if there are any more items to process. This means that
	 * this method is intended to return different results in different threads
	 * when at least one of the thread is processing. It is unlikely that it is
	 * useful to call this method during processing.
	 * @return <code>true</code> if both the thread local processing buffer and
	 * the backlog are empty.
	 */
	public boolean isEmpty() {
		return this.backlog.isEmpty() && this.processingBuffer.get().isEmpty();
	}

}
