/*
 * Copyright © 2011 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2011-present the original author or authors.
 */

package org.springframework.integration.test.support;

import org.springframework.messaging.Message;

/**
 * Validate a message. Create an anonymous instance or subclass to
 * implement the validateMessage() method
 *
 * @author David Turanski
 *
 */
public abstract class MessageValidator extends AbstractResponseValidator<Message<?>> {

	@Override
	protected final boolean extractPayload() {
		return false;
	}

	@Override
	protected final void validateResponse(Message<?> response) {
		validateMessage(response);
	}

	/**
	 * Implement this method to validate the message
	 * @param message The message.
	 */
	protected abstract void validateMessage(Message<?> message);

}
