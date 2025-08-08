/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.core;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * Base interface for any component that is capable of sending
 * messages to a {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface MessageProducer {

	/**
	 * Specify the {@link MessageChannel} to which produced Messages should be sent.
	 * @param outputChannel The output channel.
	 */
	void setOutputChannel(MessageChannel outputChannel);

	/**
	 * Specify the bean name of the {@link MessageChannel} to which produced Messages should be sent.
	 * @param outputChannel The output channel bean name.
	 * @since 5.1.2
	 */
	default void setOutputChannelName(String outputChannel) {
		throw new UnsupportedOperationException("This MessageProducer does not support setting the channel by name.");
	}

	/**
	 * Return the the output channel.
	 * @return the channel.
	 * @since 4.3
	 */
	@Nullable
	MessageChannel getOutputChannel();

}
