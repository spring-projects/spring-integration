/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
