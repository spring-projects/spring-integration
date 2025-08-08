/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.event;

/**
 * The {@link StompIntegrationEvent} indicating the STOMP session establishment.
 *
 * @author Artem Bilan
 * @since 4.2.2
 */
@SuppressWarnings("serial")
public class StompSessionConnectedEvent extends StompIntegrationEvent {

	public StompSessionConnectedEvent(Object source) {
		super(source);
	}

}
