/*
 * Copyright 2017 the original author or authors.
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

/**
 * Callback for using the same session for multiple
 * RemoteFileTemplate operations.

 * @param <F> the type the operations accepts.
 * @param <T> the type the callback returns.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
@FunctionalInterface
public interface OperationsCallback<F, T> {

	/**
	 * Execute any number of operations using a dedicated remote
	 * session as long as those operations are performed
	 * on the template argument and on the calling thread.
	 * The session will be closed when the callback exits.
	 *
	 * @param operations the RemoteFileOperations.
	 * @return the result of operations.
	 */
	T doInOperations(RemoteFileOperations<F> operations);

}
