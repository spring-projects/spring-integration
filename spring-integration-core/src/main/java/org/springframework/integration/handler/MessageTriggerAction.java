/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
