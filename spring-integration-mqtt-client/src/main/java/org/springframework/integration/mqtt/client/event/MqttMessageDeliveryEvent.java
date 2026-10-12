/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

/**
 * Base class for events related to message delivery. The Property {@link #mqttPublishResult}
 * can be used to correlate events.
 *
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public abstract class MqttMessageDeliveryEvent extends MqttIntegrationEvent {

	private final Object mqttPublishResult;

	public MqttMessageDeliveryEvent(Object source, Object mqttPublishResult) {
		super(source);
		this.mqttPublishResult = mqttPublishResult;
	}

	public Object getMqttPublishResult() {
		return this.mqttPublishResult;
	}

}
