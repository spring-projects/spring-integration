/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.jspecify.annotations.Nullable;

/**
 * The {@link MqttIntegrationEvent} to notify about lost connection to the server.
 * When normal disconnection is happened (initiated by the server), the {@code cause} is null.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jaehong Park
 *
 * @since 4.2.2
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
public class MqttConnectionFailedEvent extends MqttIntegrationEvent {

	public MqttConnectionFailedEvent(Object source) {
		super(source);
	}

	public MqttConnectionFailedEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
