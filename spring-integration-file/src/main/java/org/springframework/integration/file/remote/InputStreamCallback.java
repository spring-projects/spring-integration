/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.file.remote;

import java.io.IOException;
import java.io.InputStream;

/**
 * Callback for stream-based file retrieval using a RemoteFileOperations.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
@FunctionalInterface
public interface InputStreamCallback {

	/**
	 * Called with the InputStream for the remote file. The caller will
	 * take care of closing the stream and finalizing the file retrieval operation after
	 * this method exits.
	 * @param stream The InputStream.
	 * @throws IOException Any IOException.
	 */
	void doWithInputStream(InputStream stream) throws IOException;

}
