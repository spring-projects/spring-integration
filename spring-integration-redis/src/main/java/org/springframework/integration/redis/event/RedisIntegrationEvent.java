/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class RedisIntegrationEvent extends IntegrationEvent {

	public RedisIntegrationEvent(Object source) {
		super(source);
	}

	public RedisIntegrationEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
