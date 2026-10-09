/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.router;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageChannel;

/**
 * Routers implementing this interface have a default output channel.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface MessageRouter {

	/**
	 * Get the default output channel.
	 * @return the channel.
	 */
	@Nullable
	MessageChannel getDefaultOutputChannel();

}
