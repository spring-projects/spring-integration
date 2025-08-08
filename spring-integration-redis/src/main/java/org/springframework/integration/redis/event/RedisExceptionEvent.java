/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.event;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisExceptionEvent extends RedisIntegrationEvent {

	public RedisExceptionEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
