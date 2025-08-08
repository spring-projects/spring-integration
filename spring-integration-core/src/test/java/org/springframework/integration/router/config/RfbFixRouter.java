/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
