/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
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
