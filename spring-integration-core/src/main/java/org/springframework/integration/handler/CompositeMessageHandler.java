/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
