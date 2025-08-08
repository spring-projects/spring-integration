/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.stream;

import org.springframework.integration.events.IntegrationEvent;

/**
 * Application event published when EOF is detected on a stream.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
@SuppressWarnings("serial")
public class StreamClosedEvent extends IntegrationEvent {

	public StreamClosedEvent(Object source) {
		super(source);
	}

}
