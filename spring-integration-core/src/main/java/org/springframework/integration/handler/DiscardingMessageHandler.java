/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.handler;

import org.jspecify.annotations.Nullable;

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
