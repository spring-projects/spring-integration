/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.transformer;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Base Exception type for Message transformation errors.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SuppressWarnings("serial")
public class MessageTransformationException extends MessagingException {

	public MessageTransformationException(Message<?> message, String description, Throwable cause) {
		super(message, description, cause);
	}

	public MessageTransformationException(Message<?> message, String description) {
		super(message, description);
	}

	public MessageTransformationException(String description, Throwable cause) {
		super(description, cause);
	}

	public MessageTransformationException(String description) {
		super(description);
	}

}
