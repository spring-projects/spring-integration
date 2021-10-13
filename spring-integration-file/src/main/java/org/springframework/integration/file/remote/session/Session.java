/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Common abstraction for a Session with a remote File system.
 *
 * @param <F> the target system file type.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Alen Turkovic
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface Session<F> extends Closeable {

	boolean remove(String path) throws IOException;

	F[] list(String path) throws IOException;

	void read(String source, OutputStream outputStream) throws IOException;

	void write(InputStream inputStream, String destination) throws IOException;

	/**
	 * Append to a file.
	 * @param inputStream the stream.
	 * @param destination the destination.
	 * @throws IOException an IO Exception.
	 * @since 4.1
	 */
	void append(InputStream inputStream, String destination) throws IOException;

	boolean mkdir(String directory) throws IOException;

	/**
	 * Remove a remote directory.
	 * @param directory The directory.
	 * @return True if the directory was removed.
	 * @throws IOException an IO exception.
	 * @since 4.1
	 */
	boolean rmdir(String directory) throws IOException;

	void rename(String pathFrom, String pathTo) throws IOException;

	@Override
	void close();

	boolean isOpen();

	/**
	 * Check if the remote file or directory exists.
	 * @param path the remote path.
	 * @return {@code true} or {@code false} if remote path exists or not.
	 * @throws IOException an IO exception during remote interaction.
	 */
	boolean exists(String path) throws IOException;

	String[] listNames(String path) throws IOException;

	/**
	 * Retrieve a remote file as a raw {@link InputStream}.
	 * @param source The path of the remote file.
	 * @return The raw inputStream.
	 * @throws IOException Any IOException.
	 * @since 3.0
	 */
	InputStream readRaw(String source) throws IOException;

	/**
	 * Invoke after closing the InputStream from {@link #readRaw(String)}.
	 * Required by some session providers.
	 * @return true if successful.
	 * @throws IOException Any IOException.
	 * @since 3.0
	 */
	boolean finalizeRaw() throws IOException;

	/**
	 * Get the underlying client library's client instance for this session.
	 * Returns an {@code Object} to avoid significant changes to -file, -ftp, -sftp
	 * modules, which would be required
	 * if we added another generic parameter. Implementations should narrow the
	 * return type.
	 * @return The client instance.
	 * @since 4.1
	 */
	Object getClientInstance();

	/**
	 * Return the host:port pair this session is connected to.
	 * @return the host:port pair this session is connected to.
	 * @since 5.2
	 */
	String getHostPort();

	/**
	 * Test the session is still alive, e.g. when checking out from a pool.
	 * The default implementation simply delegates to {@link #isOpen()}.
	 * @return true if the test is successful.
	 * @since 5.1
	 */
	default boolean test() {
		return this.isOpen();
	}

	/**
	 * Mark this session as dirty, indicating that it should not be reused and any
	 * delegated sessions should be taken care of before closing.
	 * @since 5.1.2
	 * @see CachingSessionFactory.CachedSession#close()
	 */
	default void dirty() {
		// NOOP
	}

}
