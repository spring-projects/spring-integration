/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Exception that indicates no reply message is produced by a handler
 * that does have a value of true for the 'requiresReply' property.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
@SuppressWarnings("serial")
public class ReplyRequiredException extends MessagingException {

	/**
	 * @param failedMessage the failed message.
	 * @param description the description.
	 */
	public ReplyRequiredException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	/**
	 * @param failedMessage the failed message.
	 * @param description the description.
	 * @param t the root cause.
	 * @since 4.3
	 */
	public ReplyRequiredException(Message<?> failedMessage, String description, Throwable t) {
		super(failedMessage, description, t);
	}

}
