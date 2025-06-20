/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.synchronizer;

import java.io.File;

/**
 * Strategy for synchronizing from a remote File system to a local directory.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
@FunctionalInterface
public interface InboundFileSynchronizer {

	/**
	 * Synchronize all available files to the local directory.
	 * @param localDirectory the directory.
	 */
	void synchronizeToLocalDirectory(File localDirectory);

	/**
	 * Synchronize up to maxFetchSize files to the local directory.
	 * @param localDirectory the directory.
	 * @param maxFetchSize the maximum files to fetch.
	 */
	default void synchronizeToLocalDirectory(File localDirectory, int maxFetchSize) {
		synchronizeToLocalDirectory(localDirectory);
	}

}
