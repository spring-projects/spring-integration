/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.events.IntegrationEvent;

/**
 * Base class for Mqtt Events. For {@link #getSourceAsType()}, you should use a subtype
 * of {@link org.springframework.integration.mqtt.core.MqttComponent} for the receiving
 * variable.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jaehong Park
 *
 * @since 4.1
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
public abstract class MqttIntegrationEvent extends IntegrationEvent {

	public MqttIntegrationEvent(Object source) {
		super(source);
	}

	public MqttIntegrationEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
