/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.file.remote;

import org.springframework.integration.file.remote.session.Session;

/**
 * Temporary extension to {@link RemoteFileOperations} (back port).
 * Merged into {@link RemoteFileOperations} in 4.3.
 *
 * @author Gary Russell
 * @since 4.2.10
 *
 */
public interface ExtendedRemoteFileOperations<F> extends RemoteFileOperations<F> {

	/**
	 * List the files at the remote path.
	 * @param path the path.
	 * @return the list.
	 */
	F[] list(String path);

	/**
	 * Obtain a raw Session object. User must close the session when it is no longer
	 * needed.
	 * @return a session.
	 * @since 4.3
	 */
	Session<F> getSession();

}
