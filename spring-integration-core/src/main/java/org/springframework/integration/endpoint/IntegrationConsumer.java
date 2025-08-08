/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Message consumers implement this interface, the message handler within a consumer
 * may or may not emit output messages.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface IntegrationConsumer extends NamedComponent {

	/**
	 * Return the input channel.
	 * @return the input channel.
	 */
	MessageChannel getInputChannel();

	/**
	 * Return the output channel (may be null).
	 * @return the output channel.
	 */
	MessageChannel getOutputChannel();

	/**
	 * Return the consumer's handler.
	 * @return the handler.
	 */
	MessageHandler getHandler();

}
