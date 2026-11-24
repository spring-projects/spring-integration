/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

class StubDestinationResolver implements DestinationResolver<MessageChannel> {

	public MessageChannel resolveDestination(String channelName) {
		return null;
	}

}
