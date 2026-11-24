/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
