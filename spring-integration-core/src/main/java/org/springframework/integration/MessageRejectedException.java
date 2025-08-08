/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * Exception that indicates a message has been rejected by a selector.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SuppressWarnings("serial")
public class MessageRejectedException extends MessageHandlingException {

	public MessageRejectedException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	public MessageRejectedException(Message<?> failedMessage, String description, Throwable cause) {
		super(failedMessage, description, cause);
	}

}
