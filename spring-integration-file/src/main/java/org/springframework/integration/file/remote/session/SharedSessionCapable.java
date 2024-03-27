/*
 * Copyright 2013-2024 the original author or authors.
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

/**
 * A {@link SessionFactory} that implements this interface is capable of supporting a shared session.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface SharedSessionCapable {

	/**
	 * @return true if this factory uses a shared session.
	 */
	boolean isSharedSession();

	/**
	 * Resets the shared session so the next {@code #getSession()} will return a session
	 * using a new connection.
	 */
	void resetSharedSession();

}
