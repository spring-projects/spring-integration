/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Common abstraction for a Session with a remote File system.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public interface Session<T> {

	boolean remove(String path) throws IOException;

	T[] list(String path) throws IOException;

	void read(String source, OutputStream outputStream) throws IOException;

	void write(InputStream inputStream, String destination) throws IOException;

	boolean mkdir(String directory) throws IOException;

	void rename(String pathFrom, String pathTo) throws IOException;

	void close();

	boolean isOpen();

	boolean exists(String path) throws IOException;

	String[] listNames(String path) throws IOException;

	/**
	 * Retrieve a remote file as a raw {@link InputStream}.
	 * @param source The path of the remote file
	 * @return The raw inputStream.
	 */
	InputStream readRaw(String source) throws IOException;

	/**
	 * Invoke after closing the InputStream from {@link #readRaw(String)}.
	 * Required by some session providers.
	 * @return true if successful.
	 * @throws IOException
	 */
	boolean finalizeRaw() throws IOException;

}
