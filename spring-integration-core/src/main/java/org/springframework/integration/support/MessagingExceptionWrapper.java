/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A wrapper exception for a {@link MessagingException} used to convey the cause and
 * original message to a
 * {@link org.springframework.integration.channel.MessagePublishingErrorHandler}.
 * The original message is in this exception's {@link #getFailedMessage() failedMessage}
 * property.
 * <p>Intended for internal framework use only. Error handlers will typically unwrap
 * the cause while creating an error message.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class MessagingExceptionWrapper extends MessagingException {

	private static final long serialVersionUID = 1L;

	public MessagingExceptionWrapper(Message<?> originalMessage, MessagingException cause) {
		super(originalMessage, cause);
	}

}
