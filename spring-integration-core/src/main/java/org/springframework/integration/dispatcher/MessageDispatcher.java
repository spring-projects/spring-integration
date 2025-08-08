/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Strategy interface for dispatching messages to handlers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public interface MessageDispatcher {

	/**
	 * Add a message handler.
	 * @param handler the handler.
	 * @return true if successfully added.
	 */
	boolean addHandler(MessageHandler handler);

	/**
	 * Remove a message handler.
	 * @param handler the handler.
	 * @return true of successfully removed.
	 */
	boolean removeHandler(MessageHandler handler);

	/**
	 * Dispatch the message.
	 * @param message the message.
	 * @return true if dispatched.
	 */
	boolean dispatch(Message<?> message);

	/**
	 * Return the current handler count.
	 * @return the handler count.
	 */
	int getHandlerCount();

}
