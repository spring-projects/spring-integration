/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.websocket;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * The {@link ApplicationEvent} implementation to represent the
 * {@link org.springframework.web.socket.WebSocketSession} errors.
 *
 * @author Artem Bilan
 * @since 4.1
 */
@SuppressWarnings("serial")
public class SessionErrorEvent extends ApplicationEvent {

	private final String sessionId;

	private final Throwable exception;


	/**
	 * Create a new {@link ApplicationEvent} represented the error on the session.
	 * @param source the component that published the event (never {@code null})
	 * @param sessionId the id of the session
	 * @param exception the exception on the session
	 */
	public SessionErrorEvent(Object source, String sessionId, Throwable exception) {
		super(source);
		Assert.notNull(sessionId, "'sessionId' must not be null");
		this.sessionId = sessionId;
		this.exception = exception;
	}

	/**
	 * Return the session id.
	 * @return the sessionId
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Return the exception for the session id.
	 * @return the exception
	 */
	public Throwable getException() {
		return exception;
	}

	@Override
	public String toString() {
		return "SessionErrorEvent: sessionId=" + this.sessionId;
	}

}
