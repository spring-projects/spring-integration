/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.messaging.Message;

/**
 * Implementations of this interface are subclasses of
 * {@link AbstractMessageHandler} that perform post processing after the
 * {@link AbstractMessageHandler#handleMessageInternal(org.springframework.messaging.Message)}
 * call.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface PostProcessingMessageHandler {

	/**
	 * Take some further action on the result and/or message.
	 * @param result The result from {@link AbstractMessageHandler#handleMessageInternal(Message)}.
	 * @param message The message.
	 * @return The post-processed result.
	 */
	Object postProcess(Message<?> message, Object result);

}
