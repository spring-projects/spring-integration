/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.springframework.integration.events.IntegrationEvent;
import org.springframework.lang.Nullable;

/**
 * Base class for Mqtt Events. For {@link #getSourceAsType()}, you should use a subtype
 * of {@link org.springframework.integration.mqtt.core.MqttComponent} for the receiving
 * variable.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SuppressWarnings("serial")
public abstract class MqttIntegrationEvent extends IntegrationEvent {

	public MqttIntegrationEvent(Object source) {
		super(source);
	}

	public MqttIntegrationEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
