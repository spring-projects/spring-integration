/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

class StubDestinationResolver implements DestinationResolver<MessageChannel> {

	public MessageChannel resolveDestination(String channelName) {
		return null;
	}

}
