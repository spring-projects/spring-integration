/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.handler;

import java.util.List;

import org.springframework.messaging.MessageHandler;

/**
 * Classes implementing this interface delegate to a list of handlers.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface CompositeMessageHandler extends MessageHandler {

	/**
	 * Return an unmodifiable list of handlers.
	 * @return the handlers.
	 */
	List<MessageHandler> getHandlers();

}
