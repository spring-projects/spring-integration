/*
 * Copyright 2007-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.store;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Exception for problems that occur when using a {@link MessageStore} implementation.
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MessageStoreException extends MessagingException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message The message.
	 */
	public MessageStoreException(Message<?> message) {
		super(message);
	}

	/**
	 * @param description The description.
	 */
	public MessageStoreException(String description) {
		super(description);
	}

	/**
	 * @param description The description.
	 * @param cause The cause.
	 */
	public MessageStoreException(String description, Throwable cause) {
		super(description, cause);
	}

	/**
	 * @param message The message.
	 * @param description The description.
	 */
	public MessageStoreException(Message<?> message, String description) {
		super(message, description);
	}

	/**
	 * @param message The message.
	 * @param cause The cause.
	 */
	public MessageStoreException(Message<?> message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message The message.
	 * @param description The description.
	 * @param cause The cause.
	 */
	public MessageStoreException(Message<?> message, String description, Throwable cause) {
		super(message, description, cause);
	}

}
