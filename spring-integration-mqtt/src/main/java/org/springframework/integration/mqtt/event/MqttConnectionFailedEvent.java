/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.springframework.lang.Nullable;

/**
 * The {@link MqttIntegrationEvent} to notify about lost connection to the server.
 * When normal disconnection is happened (initiated by the server), the {@code cause} is null.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2.2
 *
 */
@SuppressWarnings("serial")
public class MqttConnectionFailedEvent extends MqttIntegrationEvent {

	public MqttConnectionFailedEvent(Object source) {
		super(source);
	}

	public MqttConnectionFailedEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
