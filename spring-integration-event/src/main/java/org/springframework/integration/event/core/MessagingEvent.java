/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.event.core;

import java.io.Serial;

import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;

/**
 * A subclass of {@link ApplicationEvent} that wraps a {@link Message}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessagingEvent extends ApplicationEvent {

	@Serial
	private static final long serialVersionUID = -872581247155846293L;

	/**
	 * Construct an instance based on the provided message.
	 * @param message the message for event.
	 */
	public MessagingEvent(Message<?> message) {
		super(message);
	}

	public Message<?> getMessage() {
		return (Message<?>) getSource();
	}

}
