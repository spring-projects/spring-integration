/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
