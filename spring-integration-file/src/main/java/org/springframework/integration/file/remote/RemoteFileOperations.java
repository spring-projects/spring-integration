/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.Message;

/**
 * Strategy for performing operations on remote files.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface RemoteFileOperations<F> {

	/**
	 * Send a file to a remote server, based on information in a message.
	 *
	 * @param message The message.
	 * @param mode See {@link FileExistsMode} (optional; default REPLACE). A
	 * vararg is used to make the argument optional; only the first will be
	 * used if more than one is provided.
	 * @return The remote path, or null if no local file was found.
	 */
	String send(Message<?> message, FileExistsMode... mode);

	/**
	 * Send a file to a remote server, based on information in a message.
	 * The subDirectory is appended to the remote directory evaluated from
	 * the message.
	 *
	 * @param message The message.
	 * @param subDirectory The sub directory.
	 * @param mode See {@link FileExistsMode} (optional; default REPLACE). A
	 * vararg is used to make the argument optional; only the first will be
	 * used if more than one is provided.
	 * @return The remote path, or null if no local file was found.
	 */
	String send(Message<?> message, String subDirectory, FileExistsMode... mode);

	/**
	 * Send a file to a remote server, based on information in a message, appending.
	 *
	 * @param message The message.
	 * @return The remote path, or null if no local file was found.
	 * @since 4.1
	 */
	String append(Message<?> message);

	/**
	 * Send a file to a remote server, based on information in a message, appending.
	 * The subDirectory is appended to the remote directory evaluated from
	 * the message.
	 *
	 * @param message The message.
	 * @param subDirectory The sub directory.
	 * @return The remote path, or null if no local file was found.
	 * @since 4.1
	 */
	String append(Message<?> message, String subDirectory);

	/**
	 * Retrieve a remote file as an InputStream.
	 *
	 * @param remotePath The remote path to the file.
	 * @param callback the callback.
	 * @return true if the operation was successful.
	 */
	boolean get(String remotePath, InputStreamCallback callback);

	/**
	 * Retrieve a remote file as an InputStream, based on information in a message.
	 *
	 * @param message The message which will be evaluated to generate the remote path.
	 * @param callback the callback.
	 * @return true if the operation was successful.
	 */
	boolean get(Message<?> message, InputStreamCallback callback);

	/**
	 * Check if a file exists on the remote server.
	 *
	 * @param path The full path to the file.
	 * @return true when the file exists.
	 * @since 4.1
	 */
	boolean exists(String path);

	/**
	 * Remove a remote file.
	 *
	 * @param path The full path to the file.
	 * @return true when successful.
	 */
	boolean remove(String path);

	/**
	 * Rename a remote file, creating directories if needed.
	 *
	 * @param fromPath The current path.
	 * @param toPath The new path.
	 */
	void rename(String fromPath, String toPath);

	/**
	 * Execute the callback's doInSession method after obtaining a session.
	 * Reliably closes the session when the method exits.
	 *
	 * @param callback the SessionCallback.
	 * @param <T> The type returned by
	 * {@link SessionCallback#doInSession(org.springframework.integration.file.remote.session.Session)}.
	 * @return The result of the callback method.
	 */
	<T> T execute(SessionCallback<F, T> callback);

	/**
	 * Execute the callback's doWithClient method after obtaining a session's
	 * client, providing access to low level methods.
	 * Reliably closes the session when the method exits.
	 *
	 * @param callback the ClientCallback.
	 * @param <T> The type returned by {@link ClientCallback#doWithClient(Object)}.
	 * @return The result of the callback method.
	 * @since 4.1
	 */
	<T, C> T executeWithClient(ClientCallback<C, T> callback);

}
