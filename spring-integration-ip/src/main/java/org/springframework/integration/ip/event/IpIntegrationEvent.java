/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.ip.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class IpIntegrationEvent extends IntegrationEvent {

	public IpIntegrationEvent(Object source) {
		super(source);
	}

	public IpIntegrationEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
