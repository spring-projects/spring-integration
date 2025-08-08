/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.event;

/**
 * The {@link StompIntegrationEvent} implementation for the exception from STOMP Adapters.
 *
 * @author Artem Bilan
 * @since 4.2
 */
@SuppressWarnings("serial")
public class StompExceptionEvent extends StompIntegrationEvent {

	public StompExceptionEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
