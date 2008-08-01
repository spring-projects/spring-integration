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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * Tracks changes in a directory. This implementation is thread-safe as it
 * allows to synchronously process a new directory structure.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author iwein
 */
public class DirectoryContentManager {

	private final Log logger = LogFactory.getLog(this.getClass());

	private Map<String, FileInfo> previousSnapshot = new HashMap<String, FileInfo>();

	private final Map<String, FileInfo> backlog = new HashMap<String, FileInfo>();

	/**
	 * This is the storage for backlog that is being processed by a specific
	 * thread. Not initialized means that we're not in thread safe mode (just
	 * working directly on the backlog)
	 */
	private ThreadLocal<Map<String, FileInfo>> processingBuffer = new ThreadLocal<Map<String, FileInfo>>() {
		@Override
		protected Map<String, FileInfo> initialValue() {
			return new HashMap<String, FileInfo>();
		}
	};

	public synchronized void processSnapshot(Map<String, FileInfo> currentSnapshot) {
		/*
		 * clear the threadLocal backlog. When the thread processes a new
		 * snapshot it is done with the previous message. If there are still
		 * messages in the processing buffer something is wrong because they
		 * were not processed, nor raised as failed.
		 */
		Assert.isTrue(processingBuffer.get().isEmpty());
		Iterator<Map.Entry<String, FileInfo>> iter = this.backlog.entrySet().iterator();
		while (iter.hasNext()) {
			String fileName = iter.next().getKey();
			if (!currentSnapshot.containsKey(fileName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing file '" + fileName
							+ "' from backlog. It no longer exists in remote directory.");
				}
				iter.remove();
			}
		}
		for (String fileName : currentSnapshot.keySet()) {
			if (!this.previousSnapshot.containsKey(fileName)
					|| (!this.previousSnapshot.get(fileName).equals(currentSnapshot.get(fileName)))) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding new or modified file '" + fileName + "' to backlog.");
				}
				this.backlog.put(fileName, currentSnapshot.get(fileName));
			}
		}
		this.previousSnapshot = new HashMap<String, FileInfo>(currentSnapshot);
	}

	public synchronized void fileProcessing(String... fileNames) {
		for (String fileName : fileNames) {
			if (fileName != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Moving file '" + fileName
							+ "' from the backlog to thread local backlog. It is being processed.");
				}
				processingBuffer.get().put(fileName, this.backlog.remove(fileName));
			}
		}
	}

	public synchronized void fileNotProcessed(String... fileNames) {
		for (String fileName : fileNames) {
			if (fileName != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("File '" + fileName + "' was not processed. Moving it back to the backlog");
				}
				this.backlog.put(fileName, this.processingBuffer.get().remove(fileName));
			}
		}
	}

	public synchronized void fileProcessed(String... fileNames) {
		for (String fileName : fileNames) {
			if (fileName != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing file '" + fileName + "' from the undo buffer. It has been processed.");
				}
				/*
				 * It's not relevant if clients use the thread safe approach or
				 * not at this point, so we act on both.
				 */
				this.processingBuffer.get().remove(fileName);
				this.backlog.remove(fileName);
			}
		}
	}

	public Map<String, FileInfo> getBacklog() {
		return Collections.unmodifiableMap(this.backlog);
	}

	public Map<String, FileInfo> getProcessingBuffer() {
		return Collections.unmodifiableMap(this.processingBuffer.get());
	}

}
