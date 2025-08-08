/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.messaging.Message;

/**
 * Classes implementing this interface can take some action when a trigger {@link Message}
 * is received.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
@FunctionalInterface
public interface MessageTriggerAction {

	/**
	 * Take some action based on the message.
	 * @param message the message.
	 */
	void trigger(Message<?> message);

}
