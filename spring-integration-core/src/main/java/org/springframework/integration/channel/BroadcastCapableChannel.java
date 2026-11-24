/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.channel;

import org.springframework.messaging.SubscribableChannel;

/**
 * A {@link SubscribableChannel} variant for implementations with broadcasting capabilities.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public interface BroadcastCapableChannel extends SubscribableChannel {

	/**
	 * Return a state of this channel in regards of broadcasting capabilities.
	 * @return the state of this channel in regards of broadcasting capabilities.
	 */
	default boolean isBroadcast() {
		return true;
	}

}
