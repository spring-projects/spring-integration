/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

/**
 * Exception that indicates a timeout elapsed prior to successful message delivery.
 *
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class MessageTimeoutException extends MessageDeliveryException {

	public MessageTimeoutException(String description) {
		super(description);
	}

	public MessageTimeoutException(Message<?> failedMessage, String description, Throwable cause) {
		super(failedMessage, description, cause);
	}

	public MessageTimeoutException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	public MessageTimeoutException(Message<?> failedMessage) {
		super(failedMessage);
	}

}
