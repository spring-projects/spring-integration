/*
 * Copyright 2014-present the original author or authors.
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
 * {@code RemoteFileTemplate} callback with the underlying client instance providing
 * access to lower level methods.
 *
 * @param <C> The type of the underlying client object.
 * @param <T> The return type of the callback method.
 *
 * @author Gary Russell
 *
 * @since 4.1
 */
@FunctionalInterface
public interface ClientCallback<C, T> {

	/**
	 * Called within the context of a
	 * {@link org.springframework.integration.file.remote.session.Session}. Perform some
	 * operation(s) on the client instance underlying the session. The caller will take
	 * care of closing the session after this method exits. However, the implementation is
	 * required to perform any clean up required by the client after performing
	 * operations.
	 * @param client The client instance.
	 * @return The return value.
	 */
	T doWithClient(C client);

}
