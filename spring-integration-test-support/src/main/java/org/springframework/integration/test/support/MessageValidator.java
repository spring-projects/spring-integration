/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
