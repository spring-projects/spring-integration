/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Classes implementing this interface are capable of discarding messages.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface DiscardingMessageHandler extends MessageHandler {

	/**
	 * Return the discard channel.
	 * @return the channel.
	 */
	@Nullable
	MessageChannel getDiscardChannel();

}
