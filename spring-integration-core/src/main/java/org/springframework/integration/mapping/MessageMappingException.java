/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mapping;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Exception that indicates an error during message mapping.
 *
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class MessageMappingException extends MessagingException {

	public MessageMappingException(String description) {
		super(description);
	}

	public MessageMappingException(String description, Throwable cause) {
		super(description, cause);
	}

	public MessageMappingException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	public MessageMappingException(Message<?> failedMessage, String description, Throwable cause) {
		super(failedMessage, description, cause);
	}

}
