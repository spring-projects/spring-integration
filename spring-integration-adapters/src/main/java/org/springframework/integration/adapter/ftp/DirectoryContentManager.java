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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tracks changes in a directory. This implementation is thread-safe as it
 * allows to synchronously process a new directory structure.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class DirectoryContentManager {

	private final Log logger = LogFactory.getLog(this.getClass());

	private Map<String, FileInfo> previousSnapshot = new HashMap<String, FileInfo>();

	private final Map<String, FileInfo> backlog = new HashMap<String, FileInfo>();


	public synchronized void processSnapshot(Map<String, FileInfo> currentSnapshot) {
		Iterator<Map.Entry<String, FileInfo>> iter = this.backlog.entrySet().iterator();
		while (iter.hasNext()) {
			String fileName = iter.next().getKey();
			if (!currentSnapshot.containsKey(fileName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing file '" + fileName + "' from backlog. It no longer exists in remote directory.");
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

	public synchronized void fileProcessed(String fileName) {
		if (fileName != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Removing file '" + fileName + "' from the backlog. It has been processed.");
			}
			this.backlog.remove(fileName);
		}
	}

	public Map<String, FileInfo> getBacklog() {
		return Collections.unmodifiableMap(this.backlog);
	}

}
