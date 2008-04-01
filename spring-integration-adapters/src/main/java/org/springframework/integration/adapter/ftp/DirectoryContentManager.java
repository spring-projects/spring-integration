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

/**
 * Tracks changes in the context. This implementation is thread-safe as it
 * allows to synchronously process a new directory structure.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class DirectoryContentManager {

	private Map<String, FileInfo> snapshot = new HashMap<String, FileInfo>();

	private final Map<String, FileInfo> backlog = new HashMap<String, FileInfo>();


	public synchronized void processSnapshot(Map<String, FileInfo> remoteSnapshot) {
		Iterator<Map.Entry<String, FileInfo>> iter = this.backlog.entrySet().iterator();
		while (iter.hasNext()) {
			String fileName = iter.next().getKey();
			if (!remoteSnapshot.containsKey(fileName)) {
				iter.remove();
			}
		}
		for (String fileName : remoteSnapshot.keySet()) {
			if (!this.snapshot.containsKey(fileName)
					|| (!this.snapshot.get(fileName).equals(remoteSnapshot.get(fileName)))) {
				this.backlog.put(fileName, remoteSnapshot.get(fileName));
			}
		}
		this.snapshot = new HashMap<String, FileInfo>(remoteSnapshot);
	}

	public synchronized void fileProcessed(String fileName) {
		this.backlog.remove(fileName);
	}

	public Map<String, FileInfo> getBacklog() {
		return Collections.unmodifiableMap(this.backlog);
	}

}
