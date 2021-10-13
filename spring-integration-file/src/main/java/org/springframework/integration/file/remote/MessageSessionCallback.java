/*
 * Copyright 2015-2021 the original author or authors.
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

import org.springframework.integration.file.remote.session.Session;
import org.springframework.messaging.Message;

/**
 * Callback invoked by {@code RemoteFileOperations.executeForMessage()}
 * - allows multiple operations on a session.
 *
 * @param <F> the target protocol file type.
 * @param <T> the expected execution result.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 */
@FunctionalInterface
public interface MessageSessionCallback<F, T> {

	/**
	 * Called within the context of a session and requestMessage.
	 * Perform some operation(s) on the session.
	 * The caller will take care of closing the session after this method exits.
	 *
	 * @param session The session.
	 * @param requestMessage The message to take in account with session operation(s).
	 * @return The result of type T.
	 * @throws IOException Any IOException.
	 */
	T doInSession(Session<F> session, Message<?> requestMessage) throws IOException;

}
