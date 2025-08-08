/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

/**
 * Exception that indicates an internal error occurred within a
 * {@link org.springframework.integration.dispatcher.MessageDispatcher}
 * preventing message delivery.
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
@SuppressWarnings("serial")
public class MessageDispatchingException extends MessageDeliveryException {

	public MessageDispatchingException(String description) {
		super(description);
	}

	public MessageDispatchingException(Message<?> undeliveredMessage,
			String description, Throwable cause) {
		super(undeliveredMessage, description, cause);
	}

	public MessageDispatchingException(Message<?> undeliveredMessage,
			String description) {
		super(undeliveredMessage, description);
	}

	public MessageDispatchingException(Message<?> undeliveredMessage) {
		super(undeliveredMessage);
	}

}
