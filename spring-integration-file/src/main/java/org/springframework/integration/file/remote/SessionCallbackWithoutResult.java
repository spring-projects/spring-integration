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

import java.io.IOException;

import org.springframework.integration.file.remote.session.Session;

/**
 * Simple convenience implementation of {@link SessionCallback} for cases where
 * no result is returned.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class SessionCallbackWithoutResult<F> implements SessionCallback<F, Object> {

	@Override
	public Object doInSession(Session<F> session) throws IOException {
		doInSessionWithoutResult(session);
		return null;
	}

	/**
	 * Called within the context of a session.
	 * Perform some operation(s) on the session. The caller will take
	 * care of closing the session after this method exits.
	 *
	 * @param session The session.
	 * @throws IOException Any IOException.
	 */
	protected abstract void doInSessionWithoutResult(Session<F> session) throws IOException;

}
