/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.router.config;

import java.util.Collection;

import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class RfbFixRouter extends AbstractMessageRouter {

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		return null;
	}

}
