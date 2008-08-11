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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * Tracks changes in a collection. This implementation is thread-safe as it
 * allows to synchronously process a new directory structure.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class Backlog<T> {

	private final Log logger = LogFactory.getLog(this.getClass());

	private Map<String, T> previousSnapshot = new HashMap<String, T>();

	private final Map<String, T> backlog = new HashMap<String, T>();

	/**
	 * This is the storage for backlog that is being processed by a specific
	 * thread. Not initialized means that we're not in thread safe mode (just
	 * working directly on the backlog)
	 */
	private ThreadLocal<Map<String, T>> processingBuffer = new ThreadLocal<Map<String, T>>() {
		@Override
		protected Map<String, T> initialValue() {
			return new HashMap<String, T>();
		}
	};

	public synchronized void processSnapshot(Map<String, T> currentSnapshot) {
		/*
		 * clear the threadLocal backlog. When the thread processes a new
		 * snapshot it is done with the previous message. If there are still
		 * messages in the processing buffer something is wrong because they
		 * were not processed, nor raised as failed.
		 */
		Assert.isTrue(processingBuffer.get().isEmpty(), "Processing buffer not emptied before poll.");
		Iterator<Map.Entry<String, T>> iter = this.backlog.entrySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().getKey();
			if (!currentSnapshot.containsKey(key)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing item '" + key
							+ "' from backlog. It no longer exists in remote directory.");
				}
				iter.remove();
			}
		}
		for (String key : currentSnapshot.keySet()) {
			if (!this.previousSnapshot.containsKey(key)
					|| (!this.previousSnapshot.get(key).equals(currentSnapshot.get(key)))) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding new or modified item '" + key + "' to backlog.");
				}
				this.backlog.put(key, currentSnapshot.get(key));
			}
		}
		this.previousSnapshot = new HashMap<String, T>(currentSnapshot);
	}

	public synchronized void itemProcessing(String... keys) {
		for (String key : keys) {
			if (key != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Moving item '" + key
							+ "' from the backlog to thread local backlog. It is being processed.");
				}
				processingBuffer.get().put(key, this.backlog.remove(key));
			}
		}
	}

	public synchronized void processingFailed() {
		if (logger.isDebugEnabled()) {
			logger.debug("Moving all items from processing buffer to backlog. Processing has failed");
		}
		Map<String, T> processing = this.processingBuffer.get();
		this.backlog.putAll(processing);
		processing.clear();
	}

	public synchronized void fileProcessed(String... keys) {
		for (String key : keys) {
			if (key != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing item '" + key + "' from the undo buffer. It has been processed.");
				}
				/*
				 * It's not relevant if clients use the thread safe approach or
				 * not at this point, so we act on both.
				 */
				this.processingBuffer.get().remove(key);
				this.backlog.remove(key);
			}
		}
	}

	public Map<String, T> getBacklog() {
		return Collections.unmodifiableMap(this.backlog);
	}

	public Map<String, T> getProcessingBuffer() {
		return Collections.unmodifiableMap(this.processingBuffer.get());
	}

}
